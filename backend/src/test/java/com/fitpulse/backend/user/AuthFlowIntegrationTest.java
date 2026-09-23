package com.fitpulse.backend.user;

import com.fitpulse.backend.TestcontainersConfig;
import com.fitpulse.backend.user.dto.AuthResponse;
import com.fitpulse.backend.user.dto.LoginRequest;
import com.fitpulse.backend.user.dto.RegisterRequest;
import com.fitpulse.backend.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfig.class)
class AuthFlowIntegrationTest {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void registerThenLoginThenAccessProtectedEndpoint() {
        RegisterRequest registerRequest = new RegisterRequest(
                "Marko", "Petrović", "marko@example.com", "lozinka123", LocalDate.of(1998, 5, 20));

        AuthResponse registered = restTestClient.post().uri("/api/auth/register")
                .body(registerRequest)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(registered).isNotNull();
        assertThat(registered.token()).isNotBlank();

        restTestClient.post().uri("/api/auth/register")
                .body(registerRequest)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        restTestClient.post().uri("/api/auth/login")
                .body(new LoginRequest("marko@example.com", "pogresna-lozinka"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);

        AuthResponse loggedIn = restTestClient.post().uri("/api/auth/login")
                .body(new LoginRequest("marko@example.com", "lozinka123"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(loggedIn).isNotNull();
        String token = loggedIn.token();

        restTestClient.get().uri("/api/user/me")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);

        UserResponse me = restTestClient.get().uri("/api/user/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody(UserResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(me).isNotNull();
        assertThat(me.mail()).isEqualTo("marko@example.com");
    }

    @Test
    void unknownEndpoint_shouldReturn404_notUnauthorized() {
        AuthResponse registered = restTestClient.post().uri("/api/auth/register")
                .body(new RegisterRequest("Nikola", "Nikolić", "auth-404@example.com", "lozinka123", null))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        restTestClient.get().uri("/api/ne-postoji")
                .header("Authorization", "Bearer " + registered.token())
                .exchange()
                .expectStatus().isNotFound();
    }
}
