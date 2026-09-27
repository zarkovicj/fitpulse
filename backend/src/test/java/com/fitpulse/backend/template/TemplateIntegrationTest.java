package com.fitpulse.backend.template;

import com.fitpulse.backend.TestcontainersConfig;
import com.fitpulse.backend.exercise.MuscleGroup;
import com.fitpulse.backend.exercise.dto.ExerciseRequest;
import com.fitpulse.backend.exercise.dto.ExerciseResponse;
import com.fitpulse.backend.template.dto.TemplateRequest;
import com.fitpulse.backend.template.dto.TemplateResponse;
import com.fitpulse.backend.template.dto.TemplateExerciseRequest;
import com.fitpulse.backend.template.dto.TemplateExerciseResponse;
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

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfig.class)
class TemplateIntegrationTest {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String registerAndLogin(String email) {
        return authService.register(new RegisterRequest("Test", "User", email, "lozinka123", null)).token();
    }

    private Long systemExerciseId(String name, String token) {
        List<ExerciseResponse> found = restTestClient.get().uri("/api/exercises?search=" + name)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<ExerciseResponse>>() {})
                .returnResult()
                .getResponseBody();
        return found.stream().filter(v -> v.name().equals(name)).findFirst().orElseThrow().id();
    }

    private TemplateResponse createTemplate(String token, TemplateRequest request) {
        return restTestClient.post().uri("/api/templates")
                .header("Authorization", "Bearer " + token)
                .body(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(TemplateResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private TemplateResponse updateTemplate(String token, Long id, TemplateRequest request) {
        return restTestClient.put().uri("/api/templates/" + id)
                .header("Authorization", "Bearer " + token)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TemplateResponse.class)
                .returnResult()
                .getResponseBody();
    }

    @Test
    void findAll_shouldIncludeSeededSystemTemplates() {
        String token = registerAndLogin("tpl-seed@example.com");

        List<TemplateResponse> templates = restTestClient.get().uri("/api/templates")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<TemplateResponse>>() {})
                .returnResult()
                .getResponseBody();

        TemplateResponse push = templates.stream().filter(t -> t.name().equals("Push Day")).findFirst().orElseThrow();
        assertThat(push.system()).isTrue();
        assertThat(push.exercises()).hasSize(5);
        assertThat(push.exercises().getFirst().exerciseName()).isEqualTo("Bench Press");
    }

    @Test
    void createAndUpdate_shouldSupportBothPutModesAndSubResources() {
        String token = registerAndLogin("tpl-crud@example.com");
        Long bench = systemExerciseId("Bench Press", token);
        Long squat = systemExerciseId("Squat", token);

        TemplateResponse created = createTemplate(token, new TemplateRequest("Moj trening", "description", List.of(
                new TemplateExerciseRequest(squat, 5, 5, new BigDecimal("80")),
                new TemplateExerciseRequest(bench, 3, 8, new BigDecimal("60")))));

        assertThat(created.system()).isFalse();
        assertThat(created.exercises()).extracting(TemplateExerciseResponse::exerciseName).containsExactly("Squat", "Bench Press");

        TemplateResponse renamed = updateTemplate(token, created.id(), new TemplateRequest("Novi naziv", null, null));
        assertThat(renamed.name()).isEqualTo("Novi naziv");
        assertThat(renamed.exercises()).hasSize(2);

        TemplateResponse restructured = updateTemplate(token, created.id(), new TemplateRequest("Novi naziv", null,
                List.of(new TemplateExerciseRequest(bench, 4, 6, new BigDecimal("70")))));
        assertThat(restructured.exercises()).singleElement()
                .satisfies(item -> {
                    assertThat(item.exerciseName()).isEqualTo("Bench Press");
                    assertThat(item.position()).isEqualTo(1);
                });

        TemplateExerciseResponse added = restTestClient.post().uri("/api/templates/" + created.id() + "/exercises")
                .header("Authorization", "Bearer " + token)
                .body(new TemplateExerciseRequest(squat, 3, 10, null))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(TemplateExerciseResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(added.id()).isNotNull();
        assertThat(added.position()).isEqualTo(2);

        restTestClient.delete().uri("/api/templates/" + created.id())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();

        restTestClient.get().uri("/api/templates/" + created.id())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void otherUsersTemplate_shouldBeInvisible_andSystemTemplateReadOnly() {
        String ownerToken = registerAndLogin("tpl-owner@example.com");
        String otherToken = registerAndLogin("tpl-other@example.com");

        TemplateResponse privateTemplate = createTemplate(ownerToken, new TemplateRequest("Privatni", null, null));

        restTestClient.get().uri("/api/templates/" + privateTemplate.id())
                .header("Authorization", "Bearer " + otherToken)
                .exchange()
                .expectStatus().isNotFound();

        Long systemTemplateId = restTestClient.get().uri("/api/templates")
                .header("Authorization", "Bearer " + ownerToken)
                .exchange()
                .expectBody(new ParameterizedTypeReference<List<TemplateResponse>>() {})
                .returnResult()
                .getResponseBody()
                .stream().filter(TemplateResponse::system).findFirst().orElseThrow().id();

        restTestClient.put().uri("/api/templates/" + systemTemplateId)
                .header("Authorization", "Bearer " + ownerToken)
                .body(new TemplateRequest("Hak", null, null))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void admin_shouldSeeOnlySystemTemplates() {
        String userToken = registerAndLogin("tpl-private@example.com");
        TemplateResponse privateTemplate = createTemplate(userToken, new TemplateRequest("Samo moj", null, null));

        userRepository.save(User.createAdmin("Admin", "Test", "tpl-admin@example.com",
                passwordEncoder.encode("adminlozinka")));
        String adminToken = restTestClient.post().uri("/api/auth/login")
                .body(new LoginRequest("tpl-admin@example.com", "adminlozinka"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody()
                .token();

        List<TemplateResponse> visibleToAdmin = restTestClient.get().uri("/api/templates")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<TemplateResponse>>() {})
                .returnResult()
                .getResponseBody();

        assertThat(visibleToAdmin).extracting(TemplateResponse::id).doesNotContain(privateTemplate.id());
        assertThat(visibleToAdmin).isNotEmpty().allMatch(TemplateResponse::system);

        restTestClient.get().uri("/api/templates/" + privateTemplate.id())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deletingExerciseUsedInTemplate_shouldReturnConflict() {
        String token = registerAndLogin("tpl-conflict@example.com");

        ExerciseResponse custom = restTestClient.post().uri("/api/exercises")
                .header("Authorization", "Bearer " + token)
                .body(new ExerciseRequest("Moja vežba", MuscleGroup.ARMS, null, null))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(ExerciseResponse.class)
                .returnResult()
                .getResponseBody();

        createTemplate(token, new TemplateRequest("Sa mojom vežbom", null,
                List.of(new TemplateExerciseRequest(custom.id(), 3, 10, null))));

        restTestClient.delete().uri("/api/exercises/" + custom.id())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }
}
