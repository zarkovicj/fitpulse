package com.fitpulse.backend.admin;

import com.fitpulse.backend.TestcontainersConfig;
import com.fitpulse.backend.admin.dto.AdminUserResponse;
import com.fitpulse.backend.admin.dto.UserStatusRequest;
import com.fitpulse.backend.common.ApiError;
import com.fitpulse.backend.common.PageResponse;
import com.fitpulse.backend.exercise.MuscleGroup;
import com.fitpulse.backend.exercise.dto.ExerciseRequest;
import com.fitpulse.backend.exercise.dto.ExerciseResponse;
import com.fitpulse.backend.progress.dto.WeightRequest;
import com.fitpulse.backend.template.dto.TemplateRequest;
import com.fitpulse.backend.template.dto.TemplateExerciseRequest;
import com.fitpulse.backend.user.AuthService;
import com.fitpulse.backend.user.User;
import com.fitpulse.backend.user.UserRepository;
import com.fitpulse.backend.user.dto.AuthResponse;
import com.fitpulse.backend.user.dto.LoginRequest;
import com.fitpulse.backend.user.dto.RegisterRequest;
import com.fitpulse.backend.workout.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfig.class)
class AdminUsersIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    @Autowired private RestTestClient restTestClient;
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String admin(String email) {
        userRepository.save(User.createAdmin("Admin", "Test", email, passwordEncoder.encode("adminlozinka")));
        return login(email, "adminlozinka");
    }

    private String login(String email, String password) {
        return restTestClient.post().uri("/api/auth/login")
                .body(new LoginRequest(email, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody()
                .token();
    }

    private RestTestClient.RequestHeadersSpec<?> get(String uri, String token) {
        return restTestClient.get().uri(uri).header("Authorization", "Bearer " + token);
    }

    private AdminUserResponse findUser(String adminToken, String email) {
        PageResponse<AdminUserResponse> page = get("/api/admin/users?search=" + email, adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PageResponse<AdminUserResponse>>() {})
                .returnResult()
                .getResponseBody();
        return page.content().stream().filter(u -> u.email().equals(email)).findFirst().orElse(null);
    }

    private RestTestClient.ResponseSpec setStatus(String adminToken, Long id, boolean active) {
        return restTestClient.put().uri("/api/admin/users/" + id + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .body(new UserStatusRequest(active))
                .exchange();
    }

    @Test
    void blockAndUnblock_shouldControlLoginAndCutExistingToken() {
        String adminToken = admin("block-admin@example.com");
        String userToken = authService.register(new RegisterRequest("Blok", "Test", "block-user@example.com", "lozinka123", null)).token();
        Long userId = findUser(adminToken, "block-user@example.com").id();

        AdminUserResponse blocked = setStatus(adminToken, userId, false)
                .expectStatus().isOk()
                .expectBody(AdminUserResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(blocked.active()).isFalse();

        get("/api/user/me", userToken).exchange().expectStatus().isUnauthorized();
        ApiError error = restTestClient.post().uri("/api/auth/login")
                .body(new LoginRequest("block-user@example.com", "lozinka123"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ApiError.class)
                .returnResult()
                .getResponseBody();
        assertThat(error.message()).contains("blokiran");

        setStatus(adminToken, userId, true).expectStatus().isOk();
        login("block-user@example.com", "lozinka123");
    }

    @Test
    void protections_shouldApply() {
        String adminToken = admin("guard-admin@example.com");
        admin("guard-admin2@example.com");
        String userToken = authService.register(new RegisterRequest("Obican", "Test", "guard-user@example.com", "lozinka123", null)).token();

        // korisnik nema pristup admin delu
        get("/api/admin/users", userToken).exchange().expectStatus().isForbidden();

        setStatus(adminToken, findUser(adminToken, "guard-admin@example.com").id(), false).expectStatus().isBadRequest();
        setStatus(adminToken, findUser(adminToken, "guard-admin2@example.com").id(), false).expectStatus().isForbidden();
    }

    @Test
    void delete_shouldRemoveUserWithAllTheirData() {
        String adminToken = admin("delete-admin@example.com");
        String email = "delete-user@example.com";
        String token = authService.register(new RegisterRequest("Brisanje", "Test", email, "lozinka123", LocalDate.of(1995, 5, 5))).token();

        ExerciseResponse exercise = restTestClient.post().uri("/api/exercises")
                .header("Authorization", "Bearer " + token)
                .body(new ExerciseRequest("Moja vežba", MuscleGroup.ARMS, null, null))
                .exchange()
                .expectBody(ExerciseResponse.class)
                .returnResult()
                .getResponseBody();
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.IMAGE_PNG);
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(new ByteArrayResource(PNG) {
            @Override
            public String getFilename() {
                return "image.png";
            }
        }, partHeaders));
        String imageUrl = restTestClient.put().uri("/api/exercises/" + exercise.id() + "/image")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ExerciseResponse.class)
                .returnResult()
                .getResponseBody()
                .imageUrl();

        restTestClient.post().uri("/api/templates")
                .header("Authorization", "Bearer " + token)
                .body(new TemplateRequest("Moj šablon", null,
                        List.of(new TemplateExerciseRequest(exercise.id(), 3, 10, new BigDecimal("20")))))
                .exchange()
                .expectStatus().isCreated();

        WorkoutResponse workout = restTestClient.post().uri("/api/workouts")
                .header("Authorization", "Bearer " + token)
                .body(new StartWorkoutRequest(null, List.of(new WorkoutExerciseRequest(exercise.id(), 1, 10, new BigDecimal("20")))))
                .exchange()
                .expectBody(WorkoutResponse.class)
                .returnResult()
                .getResponseBody();
        WorkoutExerciseResponse workoutExercise = workout.exercises().getFirst();
        restTestClient.put().uri("/api/workouts/{w}/exercises/{e}/sets/{s}", workout.id(), workoutExercise.id(),
                        workoutExercise.sets().getFirst().id())
                .header("Authorization", "Bearer " + token)
                .body(new WorkoutSetRequest(10, new BigDecimal("20"), true, 90))
                .exchange()
                .expectStatus().isOk();
        restTestClient.put().uri("/api/workouts/" + workout.id() + "/finish")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();
        restTestClient.put().uri("/api/progress/weight/" + LocalDate.now())
                .header("Authorization", "Bearer " + token)
                .body(new WeightRequest(new BigDecimal("80")))
                .exchange()
                .expectStatus().isOk();

        AdminUserResponse user = findUser(adminToken, email);
        assertThat(user.workoutCount()).isEqualTo(1);

        restTestClient.delete().uri("/api/admin/users/" + user.id())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNoContent();

        assertThat(findUser(adminToken, email)).isNull();
        assertThat(userRepository.findByEmail(email)).isEmpty();
        restTestClient.get().uri(imageUrl).exchange().expectStatus().isNotFound();
        get("/api/user/me", token).exchange().expectStatus().isUnauthorized();
    }
}
