package com.fitpulse.backend.template;

import com.fitpulse.backend.TestcontainersConfig;
import com.fitpulse.backend.exercise.MisicnaGrupa;
import com.fitpulse.backend.exercise.dto.VezbaRequest;
import com.fitpulse.backend.exercise.dto.VezbaResponse;
import com.fitpulse.backend.template.dto.TemplateRequest;
import com.fitpulse.backend.template.dto.TemplateResponse;
import com.fitpulse.backend.template.dto.TemplateVezbaRequest;
import com.fitpulse.backend.template.dto.TemplateVezbaResponse;
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
    private KorisnikRepository korisnikRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String registerAndLogin(String mail) {
        return authService.register(new RegisterRequest("Test", "Korisnik", mail, "lozinka123", null)).token();
    }

    private Long systemExerciseId(String naziv, String token) {
        List<VezbaResponse> found = restTestClient.get().uri("/api/exercises?search=" + naziv)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<VezbaResponse>>() {})
                .returnResult()
                .getResponseBody();
        return found.stream().filter(v -> v.naziv().equals(naziv)).findFirst().orElseThrow().id();
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

        TemplateResponse push = templates.stream().filter(t -> t.naziv().equals("Push dan")).findFirst().orElseThrow();
        assertThat(push.system()).isTrue();
        assertThat(push.exercises()).hasSize(5);
        assertThat(push.exercises().getFirst().vezbaNaziv()).isEqualTo("Bench Press");
    }

    @Test
    void createAndUpdate_shouldSupportBothPutModesAndSubResources() {
        String token = registerAndLogin("tpl-crud@example.com");
        Long bench = systemExerciseId("Bench Press", token);
        Long squat = systemExerciseId("Čučanj", token);

        TemplateResponse created = createTemplate(token, new TemplateRequest("Moj trening", "opis", false, List.of(
                new TemplateVezbaRequest(squat, 5, 5, new BigDecimal("80")),
                new TemplateVezbaRequest(bench, 3, 8, new BigDecimal("60")))));

        assertThat(created.system()).isFalse();
        assertThat(created.exercises()).extracting(TemplateVezbaResponse::vezbaNaziv).containsExactly("Čučanj", "Bench Press");

        TemplateResponse renamed = updateTemplate(token, created.id(), new TemplateRequest("Novi naziv", null, false, null));
        assertThat(renamed.naziv()).isEqualTo("Novi naziv");
        assertThat(renamed.exercises()).hasSize(2);

        TemplateResponse restructured = updateTemplate(token, created.id(), new TemplateRequest("Novi naziv", null, false,
                List.of(new TemplateVezbaRequest(bench, 4, 6, new BigDecimal("70")))));
        assertThat(restructured.exercises()).singleElement()
                .satisfies(item -> {
                    assertThat(item.vezbaNaziv()).isEqualTo("Bench Press");
                    assertThat(item.redniBroj()).isEqualTo(1);
                });

        TemplateVezbaResponse added = restTestClient.post().uri("/api/templates/" + created.id() + "/exercises")
                .header("Authorization", "Bearer " + token)
                .body(new TemplateVezbaRequest(squat, 3, 10, null))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(TemplateVezbaResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(added.id()).isNotNull();
        assertThat(added.redniBroj()).isEqualTo(2);

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

        TemplateResponse privateTemplate = createTemplate(ownerToken, new TemplateRequest("Privatni", null, false, null));

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
                .body(new TemplateRequest("Hak", null, false, null))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void admin_shouldSeeOtherUsersTemplates() {
        String userToken = registerAndLogin("tpl-private@example.com");
        TemplateResponse privateTemplate = createTemplate(userToken, new TemplateRequest("Samo moj", null, false, null));

        korisnikRepository.save(Korisnik.createAdmin("Admin", "Adminović", "tpl-admin@example.com",
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

        assertThat(visibleToAdmin).extracting(TemplateResponse::id).contains(privateTemplate.id());
    }

    @Test
    void deletingExerciseUsedInTemplate_shouldReturnConflict() {
        String token = registerAndLogin("tpl-conflict@example.com");

        VezbaResponse custom = restTestClient.post().uri("/api/exercises")
                .header("Authorization", "Bearer " + token)
                .body(new VezbaRequest("Moja vežba", MisicnaGrupa.RUKE, null, null, false))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(VezbaResponse.class)
                .returnResult()
                .getResponseBody();

        createTemplate(token, new TemplateRequest("Sa mojom vežbom", null, false,
                List.of(new TemplateVezbaRequest(custom.id(), 3, 10, null))));

        restTestClient.delete().uri("/api/exercises/" + custom.id())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }
}
