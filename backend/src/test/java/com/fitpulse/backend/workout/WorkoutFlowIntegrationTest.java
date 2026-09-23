package com.fitpulse.backend.workout;

import com.fitpulse.backend.TestcontainersConfig;
import com.fitpulse.backend.common.PageResponse;
import com.fitpulse.backend.exercise.dto.VezbaResponse;
import com.fitpulse.backend.template.dto.TemplateRequest;
import com.fitpulse.backend.template.dto.TemplateResponse;
import com.fitpulse.backend.template.dto.TemplateVezbaRequest;
import com.fitpulse.backend.template.dto.TemplateVezbaResponse;
import com.fitpulse.backend.user.AuthService;
import com.fitpulse.backend.user.dto.RegisterRequest;
import com.fitpulse.backend.workout.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfig.class)
class WorkoutFlowIntegrationTest {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private AuthService authService;

    private String registerAndLogin(String mail) {
        return authService.register(new RegisterRequest("Test", "Korisnik", mail, "lozinka123", null)).token();
    }

    private Long systemExerciseId(String naziv, String token) {
        return restTestClient.get().uri("/api/exercises?search=" + naziv)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBody(new ParameterizedTypeReference<List<VezbaResponse>>() {})
                .returnResult()
                .getResponseBody()
                .stream().filter(v -> v.naziv().equals(naziv)).findFirst().orElseThrow().id();
    }

    private TreningResponse startWorkout(String token, StartTreningRequest request) {
        return restTestClient.post().uri("/api/workouts")
                .header("Authorization", "Bearer " + token)
                .body(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(TreningResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private TreningResponse getWorkout(String token, Long id) {
        return restTestClient.get().uri("/api/workouts/" + id)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TreningResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private void completeSet(String token, Long workoutId, Long exerciseId, Long setId, int reps, String kg) {
        restTestClient.put().uri("/api/workouts/{w}/exercises/{e}/sets/{s}", workoutId, exerciseId, setId)
                .header("Authorization", "Bearer " + token)
                .body(new TreningSerijaRequest(reps, new BigDecimal(kg), true, null))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void fullWorkoutFlow_fromTemplateToFinishAndHistory() {
        String token = registerAndLogin("workout-flow@example.com");
        String otherToken = registerAndLogin("workout-other@example.com");
        Long bench = systemExerciseId("Bench Press", token);
        Long squat = systemExerciseId("Čučanj", token);

        TemplateResponse template = restTestClient.post().uri("/api/templates")
                .header("Authorization", "Bearer " + token)
                .body(new TemplateRequest("Moj push", null, false, List.of(
                        new TemplateVezbaRequest(bench, 3, 8, new BigDecimal("50")),
                        new TemplateVezbaRequest(squat, 2, 5, new BigDecimal("80")))))
                .exchange()
                .expectBody(TemplateResponse.class)
                .returnResult()
                .getResponseBody();

        // start iz template-a: vrednosti iz template-a jer istorija ne postoji
        TreningResponse workout = startWorkout(token, new StartTreningRequest(template.id(), null));
        TreningVezbaResponse benchExercise = workout.exercises().get(0);
        TreningVezbaResponse squatExercise = workout.exercises().get(1);
        assertThat(benchExercise.sets()).hasSize(3)
                .allSatisfy(set -> assertThat(set.kilaza()).isEqualByComparingTo("50"));

        restTestClient.post().uri("/api/workouts")
                .header("Authorization", "Bearer " + token)
                .body(new StartTreningRequest(template.id(), null))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        restTestClient.get().uri("/api/workouts/active")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        restTestClient.get().uri("/api/workouts/" + workout.id())
                .header("Authorization", "Bearer " + otherToken)
                .exchange()
                .expectStatus().isNotFound();

        // tokom treninga: dve završene serije, jedna dodata, jedna obrisana
        completeSet(token, workout.id(), benchExercise.id(), benchExercise.sets().get(0).id(), 10, "55");
        completeSet(token, workout.id(), benchExercise.id(), benchExercise.sets().get(1).id(), 8, "60");

        TreningSerijaResponse addedSet = restTestClient.post()
                .uri("/api/workouts/{w}/exercises/{e}/sets", workout.id(), benchExercise.id())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(TreningSerijaResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(addedSet.redniBroj()).isEqualTo(4);

        restTestClient.delete()
                .uri("/api/workouts/{w}/exercises/{e}/sets/{s}", workout.id(), squatExercise.id(), squatExercise.sets().get(1).id())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();

        assertThat(getWorkout(token, workout.id()).exercises().get(1).brojSerija()).isEqualTo(1);

        // kraj sa ažuriranjem template-a
        TreningResponse finished = restTestClient.put().uri("/api/workouts/" + workout.id() + "/finish")
                .header("Authorization", "Bearer " + token)
                .body(new FinishTreningRequest(true))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TreningResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(finished.status()).isEqualTo(StatusTreninga.COMPLETED);

        TemplateResponse updatedTemplate = restTestClient.get().uri("/api/templates/" + template.id())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBody(TemplateResponse.class)
                .returnResult()
                .getResponseBody();
        TemplateVezbaResponse benchInTemplate = updatedTemplate.exercises().get(0);
        TemplateVezbaResponse squatInTemplate = updatedTemplate.exercises().get(1);
        assertThat(benchInTemplate.brojSerija()).isEqualTo(4);
        assertThat(benchInTemplate.brojPonavljanja()).isEqualTo(8);
        assertThat(benchInTemplate.kilaza()).isEqualByComparingTo("60");
        // čučanj nije imao nijednu završenu seriju, pa ostaje kako je bio
        assertThat(squatInTemplate.brojSerija()).isEqualTo(2);
        assertThat(squatInTemplate.kilaza()).isEqualByComparingTo("80");

        completeSetExpectingConflict(token, workout.id(), benchExercise.id(), benchExercise.sets().get(2).id());

        // novi trening iz istog template-a uzima vrednosti iz prethodnog
        TreningResponse second = startWorkout(token, new StartTreningRequest(template.id(), null));
        TreningSerijaResponse firstBenchSet = second.exercises().get(0).sets().get(0);
        assertThat(firstBenchSet.brojPonavljanja()).isEqualTo(10);
        assertThat(firstBenchSet.kilaza()).isEqualByComparingTo("55");

        restTestClient.put().uri("/api/workouts/" + second.id() + "/cancel")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        restTestClient.get().uri("/api/workouts/active")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();

        // ad-hoc trening: čučanj bez zadatih vrednosti uzima vrednosti iz istorije
        TreningResponse adHoc = startWorkout(token, new StartTreningRequest(null,
                List.of(new TreningVezbaRequest(squat, null, null, null))));
        assertThat(adHoc.templateId()).isNull();
        assertThat(adHoc.exercises().getFirst().sets()).hasSize(3)
                .allSatisfy(set -> assertThat(set.brojPonavljanja()).isEqualTo(5));

        PageResponse<TreningSummaryResponse> history = restTestClient.get().uri("/api/workouts?size=10")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PageResponse<TreningSummaryResponse>>() {})
                .returnResult()
                .getResponseBody();
        assertThat(history.totalElements()).isEqualTo(3);
        assertThat(history.content().getFirst().id()).isEqualTo(adHoc.id());
        TreningSummaryResponse completed = history.content().stream()
                .filter(w -> w.id().equals(workout.id())).findFirst().orElseThrow();
        assertThat(completed.ukupnaKilaza()).isEqualByComparingTo("1030"); // 10×55 + 8×60
    }

    private void completeSetExpectingConflict(String token, Long workoutId, Long exerciseId, Long setId) {
        restTestClient.put().uri("/api/workouts/{w}/exercises/{e}/sets/{s}", workoutId, exerciseId, setId)
                .header("Authorization", "Bearer " + token)
                .body(new TreningSerijaRequest(8, new BigDecimal("50"), true, null))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }
}
