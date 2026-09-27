package com.fitpulse.backend.progress;

import com.fitpulse.backend.TestcontainersConfig;
import com.fitpulse.backend.exercise.dto.ExerciseResponse;
import com.fitpulse.backend.progress.dto.*;
import com.fitpulse.backend.user.AuthService;
import com.fitpulse.backend.user.dto.RegisterRequest;
import com.fitpulse.backend.user.dto.UserResponse;
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
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfig.class)
class ProgressIntegrationTest {

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

    private WorkoutResponse start(String token, List<WorkoutExerciseRequest> exercises) {
        return restTestClient.post().uri("/api/workouts")
                .header("Authorization", "Bearer " + token)
                .body(new StartWorkoutRequest(null, exercises))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(WorkoutResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private void completeSet(String token, WorkoutResponse workout, int exerciseIndex, int setIndex, int reps, String kg) {
        WorkoutExerciseResponse exercise = workout.exercises().get(exerciseIndex);
        restTestClient.put().uri("/api/workouts/{w}/exercises/{e}/sets/{s}",
                        workout.id(), exercise.id(), exercise.sets().get(setIndex).id())
                .header("Authorization", "Bearer " + token)
                .body(new WorkoutSetRequest(reps, kg != null ? new BigDecimal(kg) : null, true, null))
                .exchange()
                .expectStatus().isOk();
    }

    private void finish(String token, Long workoutId) {
        restTestClient.put().uri("/api/workouts/" + workoutId + "/finish")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();
    }

    private List<PersonalRecordResponse> records(String token, String query) {
        return restTestClient.get().uri("/api/progress/records" + query)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<PersonalRecordResponse>>() {})
                .returnResult()
                .getResponseBody();
    }

    private static PersonalRecordResponse find(List<PersonalRecordResponse> records, String exercise, RecordType type) {
        return records.stream().filter(r -> r.exerciseName().equals(exercise) && r.type() == type).findFirst().orElseThrow();
    }

    @Test
    void personalRecords_shouldBeCalculatedOnFinishAndOnlyImprove() {
        String token = registerAndLogin("progress-records@example.com");
        Long bench = systemExerciseId("Bench Press", token);
        Long pullUps = systemExerciseId("Pull-ups", token);

        WorkoutResponse first = start(token, List.of(
                new WorkoutExerciseRequest(bench, 3, 8, new BigDecimal("60")),
                new WorkoutExerciseRequest(pullUps, 1, 12, null)));
        completeSet(token, first, 0, 0, 8, "60");
        completeSet(token, first, 0, 1, 6, "70");
        completeSet(token, first, 1, 0, 12, null);
        finish(token, first.id());

        List<PersonalRecordResponse> fromFirst = records(token, "?workoutId=" + first.id());
        assertThat(fromFirst).hasSize(4);
        assertThat(find(fromFirst, "Bench Press", RecordType.MAX_WEIGHT).weight()).isEqualByComparingTo("70");
        assertThat(find(fromFirst, "Bench Press", RecordType.ESTIMATED_1RM).estimated1rm()).isEqualByComparingTo("84.00");
        assertThat(find(fromFirst, "Pull-ups", RecordType.MAX_REPS).reps()).isEqualTo(12);

        WorkoutResponse second = start(token, List.of(new WorkoutExerciseRequest(bench, 1, null, null)));
        completeSet(token, second, 0, 0, 10, "65");
        finish(token, second.id());

        List<PersonalRecordResponse> benchRecords = records(token, "?exerciseId=" + bench);
        assertThat(find(benchRecords, "Bench Press", RecordType.MAX_WEIGHT).workoutId()).isEqualTo(first.id());
        assertThat(find(benchRecords, "Bench Press", RecordType.ESTIMATED_1RM).estimated1rm()).isEqualByComparingTo("86.67");
        assertThat(find(benchRecords, "Bench Press", RecordType.MAX_REPS).reps()).isEqualTo(10);

        WorkoutResponse cancelled = start(token, List.of(new WorkoutExerciseRequest(bench, 1, 1, new BigDecimal("200"))));
        completeSet(token, cancelled, 0, 0, 1, "200");
        restTestClient.put().uri("/api/workouts/" + cancelled.id() + "/cancel")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();
        assertThat(find(records(token, "?exerciseId=" + bench), "Bench Press", RecordType.MAX_WEIGHT).weight())
                .isEqualByComparingTo("70");

        ProgressSummaryResponse summary = restTestClient.get().uri("/api/progress/summary")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBody(ProgressSummaryResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(summary.totalWorkouts()).isEqualTo(2);
        assertThat(summary.workoutsThisWeek()).isEqualTo(2);
        assertThat(summary.recordCount()).isEqualTo(4);
        assertThat(summary.volumeLast30Days()).isEqualByComparingTo("1550"); // 8×60 + 6×70 + 10×65

        List<ExerciseProgressResponse> benchHistory = restTestClient.get().uri("/api/progress/exercises/" + bench)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBody(new ParameterizedTypeReference<List<ExerciseProgressResponse>>() {})
                .returnResult()
                .getResponseBody();
        assertThat(benchHistory).extracting(ExerciseProgressResponse::workoutId).containsExactly(first.id(), second.id());
        assertThat(benchHistory.get(0).maxWeight()).isEqualByComparingTo("70");
        assertThat(benchHistory.get(1).best1rm()).isEqualByComparingTo("86.67");
    }

    @Test
    void weightLogAndGoal_shouldKeepProfileWeightInSync() {
        String token = registerAndLogin("progress-weight@example.com");
        LocalDate today = LocalDate.now();

        logWeight(token, today.minusDays(2), "82.5").expectStatus().isOk();
        logWeight(token, today, "81.9").expectStatus().isOk();
        logWeight(token, today, "81.7").expectStatus().isOk(); // isti dan se menja, ne duplira
        logWeight(token, today.plusDays(1), "80").expectStatus().isBadRequest();

        List<WeightLogResponse> history = restTestClient.get().uri("/api/progress/weight")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBody(new ParameterizedTypeReference<List<WeightLogResponse>>() {})
                .returnResult()
                .getResponseBody();
        assertThat(history).extracting(WeightLogResponse::date).containsExactly(today.minusDays(2), today);
        assertThat(currentWeight(token)).isEqualByComparingTo("81.7");

        restTestClient.delete().uri("/api/progress/weight/" + today)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();
        assertThat(currentWeight(token)).isEqualByComparingTo("82.5");

        BodyGoalResponse emptyGoal = restTestClient.get().uri("/api/progress/goal")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBody(BodyGoalResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(emptyGoal.weight()).isNull();

        BodyGoalResponse goal = restTestClient.put().uri("/api/progress/goal")
                .header("Authorization", "Bearer " + token)
                .body(new BodyGoalRequest(new BigDecimal("78"), new BigDecimal("15")))
                .exchange()
                .expectStatus().isOk()
                .expectBody(BodyGoalResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(goal.weight()).isEqualByComparingTo("78");
        assertThat(goal.bodyFatPercent()).isEqualByComparingTo("15");
    }

    private RestTestClient.ResponseSpec logWeight(String token, LocalDate date, String weight) {
        return restTestClient.put().uri("/api/progress/weight/" + date)
                .header("Authorization", "Bearer " + token)
                .body(new WeightRequest(new BigDecimal(weight)))
                .exchange();
    }

    private BigDecimal currentWeight(String token) {
        return restTestClient.get().uri("/api/user/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBody(UserResponse.class)
                .returnResult()
                .getResponseBody()
                .weight();
    }
}
