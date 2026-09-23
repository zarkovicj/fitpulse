package com.fitpulse.backend.exercise;

import com.fitpulse.backend.TestcontainersConfig;
import com.fitpulse.backend.exercise.dto.VezbaRequest;
import com.fitpulse.backend.exercise.dto.VezbaResponse;
import com.fitpulse.backend.user.AuthService;
import com.fitpulse.backend.user.Korisnik;
import com.fitpulse.backend.user.KorisnikRepository;
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
    private KorisnikRepository korisnikRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String registerAndLogin(String mail) {
        RegisterRequest request = new RegisterRequest("Test", "Korisnik", mail, "lozinka123", LocalDate.of(2000, 1, 1));
        return authService.register(request).token();
    }

    private String createAdminAndLogin(String mail) {
        Korisnik admin = Korisnik.createAdmin("Admin", "Adminovic", mail, passwordEncoder.encode("adminlozinka"));
        korisnikRepository.save(admin);

        AuthResponse response = restTestClient.post().uri("/api/auth/login")
                .body(new LoginRequest(mail, "adminlozinka"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();
        return response.token();
    }

    @Test
    void ownershipRulesAreEnforced() {
        String user1Token = registerAndLogin("user1@example.com");
        String user2Token = registerAndLogin("user2@example.com");
        String adminToken = createAdminAndLogin("admin@example.com");

        VezbaRequest userExerciseRequest = new VezbaRequest("Čučanj sa šipkom", MisicnaGrupa.NOGE, null, null, false);
        VezbaResponse userExercise = restTestClient.post().uri("/api/exercises")
                .header("Authorization", "Bearer " + user1Token)
                .body(userExerciseRequest)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(VezbaResponse.class)
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
                .body(new VezbaRequest("Izmenjen naziv", MisicnaGrupa.NOGE, null, null, false))
                .exchange()
                .expectStatus().isNotFound();

        List<VezbaResponse> visibleToUser2 = restTestClient.get().uri("/api/exercises")
                .header("Authorization", "Bearer " + user2Token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<VezbaResponse>>() {})
                .returnResult()
                .getResponseBody();
        assertThat(visibleToUser2).extracting(VezbaResponse::id).doesNotContain(exerciseId);

        VezbaRequest systemExerciseRequest = new VezbaRequest("Mrtvo dizanje", MisicnaGrupa.LEDJA, null, null, true);
        VezbaResponse systemExercise = restTestClient.post().uri("/api/exercises")
                .header("Authorization", "Bearer " + adminToken)
                .body(systemExerciseRequest)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(VezbaResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(systemExercise).isNotNull();
        assertThat(systemExercise.system()).isTrue();
        assertThat(systemExercise.createdBy()).isNull();

        restTestClient.put().uri("/api/exercises/" + systemExercise.id())
                .header("Authorization", "Bearer " + user1Token)
                .body(new VezbaRequest("Pokušaj izmene", MisicnaGrupa.LEDJA, null, null, false))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);

        VezbaResponse adminUpdated = restTestClient.put().uri("/api/exercises/" + exerciseId)
                .header("Authorization", "Bearer " + adminToken)
                .body(new VezbaRequest("Admin izmenio", MisicnaGrupa.NOGE, null, null, false))
                .exchange()
                .expectStatus().isOk()
                .expectBody(VezbaResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(adminUpdated).isNotNull();
        assertThat(adminUpdated.naziv()).isEqualTo("Admin izmenio");

        List<VezbaResponse> visibleToAdmin = restTestClient.get().uri("/api/exercises")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<VezbaResponse>>() {})
                .returnResult()
                .getResponseBody();
        assertThat(visibleToAdmin).extracting(VezbaResponse::id).contains(exerciseId, systemExercise.id());

        restTestClient.get().uri("/api/exercises?muscleGroup=NEPOSTOJI")
                .header("Authorization", "Bearer " + user1Token)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
