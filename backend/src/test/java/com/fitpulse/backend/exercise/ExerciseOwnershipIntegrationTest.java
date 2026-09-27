package com.fitpulse.backend.exercise;

import com.fitpulse.backend.TestcontainersConfig;
import com.fitpulse.backend.exercise.dto.ExerciseRequest;
import com.fitpulse.backend.exercise.dto.ExerciseResponse;
import com.fitpulse.backend.user.AuthService;
import com.fitpulse.backend.user.User;
import com.fitpulse.backend.user.UserRepository;
import com.fitpulse.backend.user.dto.AuthResponse;
import com.fitpulse.backend.user.dto.LoginRequest;
import com.fitpulse.backend.user.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfig.class)
class ExerciseOwnershipIntegrationTest {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String registerAndLogin(String email) {
        RegisterRequest request = new RegisterRequest("Test", "User", email, "lozinka123", LocalDate.of(2000, 1, 1));
        return authService.register(request).token();
    }

    private String createAdminAndLogin(String email) {
        User admin = User.createAdmin("Admin", "Test", email, passwordEncoder.encode("adminlozinka"));
        userRepository.save(admin);

        AuthResponse response = restTestClient.post().uri("/api/auth/login")
                .body(new LoginRequest(email, "adminlozinka"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();
        return response.token();
    }

    @Test
    void videoUrl_shouldBeSavedAndInvalidLinkRejected() {
        String token = registerAndLogin("video@example.com");
        String video = "https://www.youtube.com/watch?v=rT7DgCr-3pg";

        ExerciseResponse created = restTestClient.post().uri("/api/exercises")
                .header("Authorization", "Bearer " + token)
                .body(new ExerciseRequest("Kosi potisak", MuscleGroup.CHEST, null, video))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(ExerciseResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(created.videoUrl()).isEqualTo(video);

        restTestClient.put().uri("/api/exercises/" + created.id())
                .header("Authorization", "Bearer " + token)
                .body(new ExerciseRequest("Kosi potisak", MuscleGroup.CHEST, null, "javascript:alert(1)"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ownershipRulesAreEnforced() {
        String user1Token = registerAndLogin("user1@example.com");
        String user2Token = registerAndLogin("user2@example.com");
        String adminToken = createAdminAndLogin("admin@example.com");

        ExerciseRequest userExerciseRequest = new ExerciseRequest("Front Squat", MuscleGroup.LEGS, null, null);
        ExerciseResponse userExercise = restTestClient.post().uri("/api/exercises")
                .header("Authorization", "Bearer " + user1Token)
                .body(userExerciseRequest)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(ExerciseResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(userExercise).isNotNull();
        assertThat(userExercise.system()).isFalse();
        Long exerciseId = userExercise.id();

        restTestClient.get().uri("/api/exercises/" + exerciseId)
                .header("Authorization", "Bearer " + user2Token)
                .exchange()
                .expectStatus().isNotFound();

        restTestClient.put().uri("/api/exercises/" + exerciseId)
                .header("Authorization", "Bearer " + user2Token)
                .body(new ExerciseRequest("Izmenjen naziv", MuscleGroup.LEGS, null, null))
                .exchange()
                .expectStatus().isNotFound();

        List<ExerciseResponse> visibleToUser2 = restTestClient.get().uri("/api/exercises")
                .header("Authorization", "Bearer " + user2Token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<ExerciseResponse>>() {})
                .returnResult()
                .getResponseBody();
        assertThat(visibleToUser2).extracting(ExerciseResponse::id).doesNotContain(exerciseId);

        ExerciseRequest systemExerciseRequest = new ExerciseRequest("Romanian Deadlift", MuscleGroup.BACK, null, null);
        ExerciseResponse systemExercise = restTestClient.post().uri("/api/exercises")
                .header("Authorization", "Bearer " + adminToken)
                .body(systemExerciseRequest)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(ExerciseResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(systemExercise).isNotNull();
        assertThat(systemExercise.system()).isTrue();
        assertThat(systemExercise.createdBy()).isNull();

        restTestClient.put().uri("/api/exercises/" + systemExercise.id())
                .header("Authorization", "Bearer " + user1Token)
                .body(new ExerciseRequest("Pokušaj izmene", MuscleGroup.BACK, null, null))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);

        ExerciseResponse adminUpdated = restTestClient.put().uri("/api/exercises/" + systemExercise.id())
                .header("Authorization", "Bearer " + adminToken)
                .body(new ExerciseRequest("Rumunsko mrtvo dizanje", MuscleGroup.BACK, null, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ExerciseResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(adminUpdated.name()).isEqualTo("Rumunsko mrtvo dizanje");

        restTestClient.put().uri("/api/exercises/" + exerciseId)
                .header("Authorization", "Bearer " + adminToken)
                .body(new ExerciseRequest("Admin izmenio", MuscleGroup.LEGS, null, null))
                .exchange()
                .expectStatus().isNotFound();

        List<ExerciseResponse> visibleToAdmin = restTestClient.get().uri("/api/exercises")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<ExerciseResponse>>() {})
                .returnResult()
                .getResponseBody();
        assertThat(visibleToAdmin).extracting(ExerciseResponse::id).contains(systemExercise.id()).doesNotContain(exerciseId);
        assertThat(visibleToAdmin).allMatch(ExerciseResponse::system);

        restTestClient.get().uri("/api/workouts")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isForbidden();
        restTestClient.get().uri("/api/progress/summary")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isForbidden();

        restTestClient.get().uri("/api/exercises?muscleGroup=NEPOSTOJI")
                .header("Authorization", "Bearer " + user1Token)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
