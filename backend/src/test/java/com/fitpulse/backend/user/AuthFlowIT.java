package com.fitpulse.backend.user;

import com.fitpulse.backend.user.dto.AuthResponse;
import com.fitpulse.backend.user.dto.LoginRequest;
import com.fitpulse.backend.user.dto.RegisterRequest;
import com.fitpulse.backend.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AuthFlowIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void registerThenLoginThenAccessProtectedEndpoint() {
        RegisterRequest registerRequest = new RegisterRequest(
                "Marko", "Petrović", "marko@example.com", "lozinka123", LocalDate.of(1998, 5, 20));

        ResponseEntity<AuthResponse> registerResponse =
                restTemplate.postForEntity("/api/auth/register", registerRequest, AuthResponse.class);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().token()).isNotBlank();

        ResponseEntity<Object> duplicateResponse =
                restTemplate.postForEntity("/api/auth/register", registerRequest, Object.class);
        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        LoginRequest wrongPassword = new LoginRequest("marko@example.com", "pogresna-lozinka");
        ResponseEntity<Object> wrongLoginResponse =
                restTemplate.postForEntity("/api/auth/login", wrongPassword, Object.class);
        assertThat(wrongLoginResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        LoginRequest loginRequest = new LoginRequest("marko@example.com", "lozinka123");
        ResponseEntity<AuthResponse> loginResponse =
                restTemplate.postForEntity("/api/auth/login", loginRequest, AuthResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResponse.getBody().token();

        ResponseEntity<Object> noTokenResponse = restTemplate.getForEntity("/api/user/me", Object.class);
        assertThat(noTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<UserResponse> meResponse = restTemplate.exchange(
                "/api/user/me", org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), UserResponse.class);

        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody().mail()).isEqualTo("marko@example.com");
    }
}
