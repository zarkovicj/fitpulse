package com.fitpulse.backend.progress;

import com.fitpulse.backend.TestcontainersConfig;
import com.fitpulse.backend.exercise.dto.VezbaResponse;
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

    private TreningResponse start(String token, List<TreningVezbaRequest> exercises) {
        return restTestClient.post().uri("/api/workouts")
                .header("Authorization", "Bearer " + token)
                .body(new StartTreningRequest(null, exercises))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(TreningResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private void completeSet(String token, TreningResponse workout, int exerciseIndex, int setIndex, int reps, String kg) {
        TreningVezbaResponse exercise = workout.exercises().get(exerciseIndex);
        restTestClient.put().uri("/api/workouts/{w}/exercises/{e}/sets/{s}",
                        workout.id(), exercise.id(), exercise.sets().get(setIndex).id())
                .header("Authorization", "Bearer " + token)
                .body(new TreningSerijaRequest(reps, kg != null ? new BigDecimal(kg) : null, true, null))
                .exchange()
                .expectStatus().isOk();
    }

    private void finish(String token, Long workoutId) {
        restTestClient.put().uri("/api/workouts/" + workoutId + "/finish")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();
    }

    private List<LicniRekordResponse> records(String token, String query) {
        return restTestClient.get().uri("/api/progress/records" + query)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<LicniRekordResponse>>() {})
                .returnResult()
                .getResponseBody();
    }

    private static LicniRekordResponse find(List<LicniRekordResponse> records, String vezba, TipRekorda tip) {
        return records.stream().filter(r -> r.vezbaNaziv().equals(vezba) && r.tip() == tip).findFirst().orElseThrow();
    }

    @Test
    void personalRecords_shouldBeCalculatedOnFinishAndOnlyImprove() {
        String token = registerAndLogin("progress-records@example.com");
        Long bench = systemExerciseId("Bench Press", token);
        Long pullUps = systemExerciseId("Zgibovi", token);

        // prvi trening: bench 8×60 i 6×70, zgibovi 12 bez tega
        TreningResponse first = start(token, List.of(
                new TreningVezbaRequest(bench, 3, 8, new BigDecimal("60")),
                new TreningVezbaRequest(pullUps, 1, 12, null)));
        completeSet(token, first, 0, 0, 8, "60");
        completeSet(token, first, 0, 1, 6, "70");
        completeSet(token, first, 1, 0, 12, null);
        finish(token, first.id());

        List<LicniRekordResponse> fromFirst = records(token, "?treningId=" + first.id());
        assertThat(fromFirst).hasSize(4);
        assertThat(find(fromFirst, "Bench Press", TipRekorda.MAX_WEIGHT).kilaza()).isEqualByComparingTo("70");
        assertThat(find(fromFirst, "Bench Press", TipRekorda.ESTIMATED_1RM).estimated1rm()).isEqualByComparingTo("84.00");
        assertThat(find(fromFirst, "Zgibovi", TipRekorda.MAX_REPS).ponavljanja()).isEqualTo(12);

        // drugi trening: 10×65 obara 1RM i ponavljanja, ali ne i najveću kilažu
        TreningResponse second = start(token, List.of(new TreningVezbaRequest(bench, 1, null, null)));
        completeSet(token, second, 0, 0, 10, "65");
        finish(token, second.id());

        List<LicniRekordResponse> benchRecords = records(token, "?vezbaId=" + bench);
        assertThat(find(benchRecords, "Bench Press", TipRekorda.MAX_WEIGHT).treningId()).isEqualTo(first.id());
        assertThat(find(benchRecords, "Bench Press", TipRekorda.ESTIMATED_1RM).estimated1rm()).isEqualByComparingTo("86.67");
        assertThat(find(benchRecords, "Bench Press", TipRekorda.MAX_REPS).ponavljanja()).isEqualTo(10);

        // otkazan trening ne pravi rekorde, ma koliko teška serija bila
        TreningResponse cancelled = start(token, List.of(new TreningVezbaRequest(bench, 1, 1, new BigDecimal("200"))));
        completeSet(token, cancelled, 0, 0, 1, "200");
        restTestClient.put().uri("/api/workouts/" + cancelled.id() + "/cancel")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();
        assertThat(find(records(token, "?vezbaId=" + bench), "Bench Press", TipRekorda.MAX_WEIGHT).kilaza())
                .isEqualByComparingTo("70");

        ProgressSummaryResponse summary = restTestClient.get().uri("/api/progress/summary")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBody(ProgressSummaryResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(summary.ukupnoTreninga()).isEqualTo(2);
        assertThat(summary.treninziOveNedelje()).isEqualTo(2);
        assertThat(summary.brojRekorda()).isEqualTo(4);
        assertThat(summary.ukupnaKilaza30Dana()).isEqualByComparingTo("1550"); // 8×60 + 6×70 + 10×65

        List<VezbaNapredakResponse> benchHistory = restTestClient.get().uri("/api/progress/exercises/" + bench)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBody(new ParameterizedTypeReference<List<VezbaNapredakResponse>>() {})
                .returnResult()
                .getResponseBody();
        assertThat(benchHistory).extracting(VezbaNapredakResponse::treningId).containsExactly(first.id(), second.id());
        assertThat(benchHistory.get(0).maxKilaza()).isEqualByComparingTo("70");
        assertThat(benchHistory.get(1).najbolji1rm()).isEqualByComparingTo("86.67");
    }

    @Test
    void weightLogAndGoal_shouldKeepProfileWeightInSync() {
        String token = registerAndLogin("progress-weight@example.com");
        LocalDate today = LocalDate.now();

        logWeight(token, today.minusDays(2), "82.5").expectStatus().isOk();
        logWeight(token, today, "81.9").expectStatus().isOk();
        logWeight(token, today, "81.7").expectStatus().isOk(); // isti dan se menja, ne duplira
        logWeight(token, today.plusDays(1), "80").expectStatus().isBadRequest();

        List<MasaLogResponse> history = restTestClient.get().uri("/api/progress/weight")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBody(new ParameterizedTypeReference<List<MasaLogResponse>>() {})
                .returnResult()
                .getResponseBody();
        assertThat(history).extracting(MasaLogResponse::datum).containsExactly(today.minusDays(2), today);
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
        assertThat(emptyGoal.masa()).isNull();

        BodyGoalResponse goal = restTestClient.put().uri("/api/progress/goal")
                .header("Authorization", "Bearer " + token)
                .body(new BodyGoalRequest(new BigDecimal("78"), new BigDecimal("15")))
                .exchange()
                .expectStatus().isOk()
                .expectBody(BodyGoalResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(goal.masa()).isEqualByComparingTo("78");
        assertThat(goal.procenatMasti()).isEqualByComparingTo("15");
    }

    private RestTestClient.ResponseSpec logWeight(String token, LocalDate datum, String masa) {
        return restTestClient.put().uri("/api/progress/weight/" + datum)
                .header("Authorization", "Bearer " + token)
                .body(new MasaRequest(new BigDecimal(masa)))
                .exchange();
    }

    private BigDecimal currentWeight(String token) {
        return restTestClient.get().uri("/api/user/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBody(UserResponse.class)
                .returnResult()
                .getResponseBody()
                .masa();
    }
}
