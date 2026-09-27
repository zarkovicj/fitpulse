package com.fitpulse.backend.workout;

import com.fitpulse.backend.TestcontainersConfig;
import com.fitpulse.backend.common.PageResponse;
import com.fitpulse.backend.exercise.dto.ExerciseResponse;
import com.fitpulse.backend.template.dto.TemplateRequest;
import com.fitpulse.backend.template.dto.TemplateResponse;
import com.fitpulse.backend.template.dto.TemplateExerciseRequest;
import com.fitpulse.backend.template.dto.TemplateExerciseResponse;
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

    private String registerAndLogin(String email) {
        return authService.register(new RegisterRequest("Test", "User", email, "lozinka123", null)).token();
    }

    private Long systemExerciseId(String name, String token) {
        return restTestClient.get().uri("/api/exercises?search=" + name)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBody(new ParameterizedTypeReference<List<ExerciseResponse>>() {})
                .returnResult()
                .getResponseBody()
                .stream().filter(v -> v.name().equals(name)).findFirst().orElseThrow().id();
    }

    private WorkoutResponse startWorkout(String token, StartWorkoutRequest request) {
        return restTestClient.post().uri("/api/workouts")
                .header("Authorization", "Bearer " + token)
                .body(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(WorkoutResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private WorkoutResponse getWorkout(String token, Long id) {
        return restTestClient.get().uri("/api/workouts/" + id)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WorkoutResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private void completeSet(String token, Long workoutId, Long exerciseId, Long setId, int reps, String kg) {
        restTestClient.put().uri("/api/workouts/{w}/exercises/{e}/sets/{s}", workoutId, exerciseId, setId)
                .header("Authorization", "Bearer " + token)
                .body(new WorkoutSetRequest(reps, new BigDecimal(kg), true, null))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void fullWorkoutFlow_fromTemplateToFinishAndHistory() {
        String token = registerAndLogin("workout-flow@example.com");
        String otherToken = registerAndLogin("workout-other@example.com");
        Long bench = systemExerciseId("Bench Press", token);
        Long squat = systemExerciseId("Squat", token);

        TemplateResponse template = restTestClient.post().uri("/api/templates")
                .header("Authorization", "Bearer " + token)
                .body(new TemplateRequest("Moj push", null, List.of(
                        new TemplateExerciseRequest(bench, 3, 8, new BigDecimal("50")),
                        new TemplateExerciseRequest(squat, 2, 5, new BigDecimal("80")))))
                .exchange()
                .expectBody(TemplateResponse.class)
                .returnResult()
                .getResponseBody();

        WorkoutResponse workout = startWorkout(token, new StartWorkoutRequest(template.id(), null));
        WorkoutExerciseResponse benchExercise = workout.exercises().get(0);
        WorkoutExerciseResponse squatExercise = workout.exercises().get(1);
        assertThat(benchExercise.sets()).hasSize(3)
                .allSatisfy(set -> assertThat(set.weight()).isEqualByComparingTo("50"));

        restTestClient.post().uri("/api/workouts")
                .header("Authorization", "Bearer " + token)
                .body(new StartWorkoutRequest(template.id(), null))
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

        completeSet(token, workout.id(), benchExercise.id(), benchExercise.sets().get(0).id(), 10, "55");
        completeSet(token, workout.id(), benchExercise.id(), benchExercise.sets().get(1).id(), 8, "60");

        WorkoutSetResponse addedSet = restTestClient.post()
                .uri("/api/workouts/{w}/exercises/{e}/sets", workout.id(), benchExercise.id())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(WorkoutSetResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(addedSet.position()).isEqualTo(4);

        restTestClient.delete()
                .uri("/api/workouts/{w}/exercises/{e}/sets/{s}", workout.id(), squatExercise.id(), squatExercise.sets().get(1).id())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();

        assertThat(getWorkout(token, workout.id()).exercises().get(1).setCount()).isEqualTo(1);

        WorkoutResponse finished = restTestClient.put().uri("/api/workouts/" + workout.id() + "/finish")
                .header("Authorization", "Bearer " + token)
                .body(new FinishWorkoutRequest(true))
                .exchange()
                .expectStatus().isOk()
                .expectBody(WorkoutResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(finished.status()).isEqualTo(WorkoutStatus.COMPLETED);

        TemplateResponse updatedTemplate = restTestClient.get().uri("/api/templates/" + template.id())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBody(TemplateResponse.class)
                .returnResult()
                .getResponseBody();
        TemplateExerciseResponse benchInTemplate = updatedTemplate.exercises().get(0);
        TemplateExerciseResponse squatInTemplate = updatedTemplate.exercises().get(1);
        assertThat(benchInTemplate.setCount()).isEqualTo(4);
        assertThat(benchInTemplate.reps()).isEqualTo(8);
        assertThat(benchInTemplate.weight()).isEqualByComparingTo("60");
        assertThat(squatInTemplate.setCount()).isEqualTo(2);
        assertThat(squatInTemplate.weight()).isEqualByComparingTo("80");

        completeSetExpectingConflict(token, workout.id(), benchExercise.id(), benchExercise.sets().get(2).id());

        WorkoutResponse second = startWorkout(token, new StartWorkoutRequest(template.id(), null));
        WorkoutSetResponse firstBenchSet = second.exercises().get(0).sets().get(0);
        assertThat(firstBenchSet.reps()).isEqualTo(10);
        assertThat(firstBenchSet.weight()).isEqualByComparingTo("55");

        restTestClient.put().uri("/api/workouts/" + second.id() + "/cancel")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        restTestClient.get().uri("/api/workouts/active")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();

        WorkoutResponse adHoc = startWorkout(token, new StartWorkoutRequest(null,
                List.of(new WorkoutExerciseRequest(squat, null, null, null))));
        assertThat(adHoc.templateId()).isNull();
        assertThat(adHoc.exercises().getFirst().sets()).hasSize(3)
                .allSatisfy(set -> assertThat(set.reps()).isEqualTo(5));

        PageResponse<WorkoutSummaryResponse> history = restTestClient.get().uri("/api/workouts?size=10")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PageResponse<WorkoutSummaryResponse>>() {})
                .returnResult()
                .getResponseBody();
        assertThat(history.totalElements()).isEqualTo(3);
        assertThat(history.content().getFirst().id()).isEqualTo(adHoc.id());
        WorkoutSummaryResponse completed = history.content().stream()
                .filter(w -> w.id().equals(workout.id())).findFirst().orElseThrow();
        assertThat(completed.totalVolume()).isEqualByComparingTo("1030"); // 10×55 + 8×60
    }

    @Test
    void limits_shouldRejectHugeValues_andExtremeSetShouldNotBreakFinish() {
        String token = registerAndLogin("workout-limits@example.com");
        Long bench = systemExerciseId("Bench Press", token);

        restTestClient.post().uri("/api/workouts")
                .header("Authorization", "Bearer " + token)
                .body(new StartWorkoutRequest(null, List.of(new WorkoutExerciseRequest(bench, 1_000_000, 10, null))))
                .exchange()
                .expectStatus().isBadRequest();

        WorkoutResponse workout = startWorkout(token, new StartWorkoutRequest(null,
                List.of(new WorkoutExerciseRequest(bench, 20, 10, new BigDecimal("50")))));
        WorkoutExerciseResponse exercise = workout.exercises().getFirst();

        restTestClient.post().uri("/api/workouts/{w}/exercises/{e}/sets", workout.id(), exercise.id())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isBadRequest();

        restTestClient.put().uri("/api/workouts/{w}/exercises/{e}/sets/{s}", workout.id(), exercise.id(), exercise.sets().get(0).id())
                .header("Authorization", "Bearer " + token)
                .body(new WorkoutSetRequest(10, new BigDecimal("10000"), true, null))
                .exchange()
                .expectStatus().isBadRequest();

        completeSet(token, workout.id(), exercise.id(), exercise.sets().get(0).id(), 1000, "300");

        restTestClient.put().uri("/api/workouts/" + workout.id() + "/finish")
                .header("Authorization", "Bearer " + token)
                .body(new FinishWorkoutRequest(false))
                .exchange()
                .expectStatus().isOk();
    }

    private void completeSetExpectingConflict(String token, Long workoutId, Long exerciseId, Long setId) {
        restTestClient.put().uri("/api/workouts/{w}/exercises/{e}/sets/{s}", workoutId, exerciseId, setId)
                .header("Authorization", "Bearer " + token)
                .body(new WorkoutSetRequest(8, new BigDecimal("50"), true, null))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }
}
