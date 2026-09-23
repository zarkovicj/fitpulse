package com.fitpulse.backend.progress;

import com.fitpulse.backend.exercise.MisicnaGrupa;
import com.fitpulse.backend.exercise.Vezba;
import com.fitpulse.backend.user.Korisnik;
import com.fitpulse.backend.workout.Trening;
import com.fitpulse.backend.workout.TreningRepository;
import com.fitpulse.backend.workout.TreningSerija;
import com.fitpulse.backend.workout.TreningVezba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LicniRekordServiceTest {

    @Mock private LicniRekordRepository rekordRepository;
    @Mock private TreningRepository treningRepository;

    private LicniRekordService service;
    private Korisnik korisnik;
    private Vezba bench;
    private Trening trening;

    @BeforeEach
    void setUp() {
        service = new LicniRekordService(rekordRepository, treningRepository);

        korisnik = Korisnik.register("Pera", "Perić", "pera@example.com", "hash", null);
        korisnik.setId(1L);
        bench = Vezba.create("Bench Press", MisicnaGrupa.GRUDI, null, null, null);
        bench.setId(10L);
        Vezba zgibovi = Vezba.create("Zgibovi", MisicnaGrupa.LEDJA, null, null, null);
        zgibovi.setId(11L);

        // bench: 8×60 i 6×70 završene, 12×40 nije; zgibovi: 12 bez tega
        trening = Trening.start(korisnik, null);
        TreningVezba benchExercise = trening.addExercise(bench);
        complete(benchExercise, benchExercise.addSet(8, new BigDecimal("60")));
        complete(benchExercise, benchExercise.addSet(6, new BigDecimal("70")));
        benchExercise.addSet(12, new BigDecimal("40"));
        TreningVezba pullUps = trening.addExercise(zgibovi);
        complete(pullUps, pullUps.addSet(12, null));
        trening.finish();

        when(rekordRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static void complete(TreningVezba exercise, TreningSerija set) {
        exercise.updateSet(set, set.getBrojPonavljanja(), set.getKilaza(), true, null);
    }

    private static LicniRekord find(List<LicniRekord> records, String vezba, TipRekorda tip) {
        return records.stream()
                .filter(r -> r.getVezba().getNaziv().equals(vezba) && r.getTip() == tip)
                .findFirst().orElse(null);
    }

    @Test
    void evaluate_withoutExistingRecords_shouldCreateRecordsFromCompletedSetsOnly() {
        when(rekordRepository.findByKorisnikIdAndVezbaIdIn(eq(1L), any())).thenReturn(List.of());

        List<LicniRekord> records = service.evaluate(trening);

        assertThat(records).hasSize(4);
        assertThat(find(records, "Bench Press", TipRekorda.MAX_WEIGHT).getKilaza()).isEqualByComparingTo("70");
        assertThat(find(records, "Bench Press", TipRekorda.ESTIMATED_1RM).getEstimated1rm()).isEqualByComparingTo("84.00");
        // 12×40 nije završena, pa se ne računa
        assertThat(find(records, "Bench Press", TipRekorda.MAX_REPS).getPonavljanja()).isEqualTo(8);
        LicniRekord pullUpReps = find(records, "Zgibovi", TipRekorda.MAX_REPS);
        assertThat(pullUpReps.getPonavljanja()).isEqualTo(12);
        assertThat(pullUpReps.getKilaza()).isNull();
        assertThat(find(records, "Zgibovi", TipRekorda.MAX_WEIGHT)).isNull();
    }

    @Test
    void evaluate_withExistingRecords_shouldUpdateOnlyThoseThatWereBeaten() {
        LicniRekord heavierWeight = LicniRekord.create(korisnik, bench, TipRekorda.MAX_WEIGHT);
        heavierWeight.record(3, new BigDecimal("80"), new BigDecimal("88.00"), 99L, Instant.now());
        LicniRekord weaker1rm = LicniRekord.create(korisnik, bench, TipRekorda.ESTIMATED_1RM);
        weaker1rm.record(5, new BigDecimal("65"), new BigDecimal("75.83"), 99L, Instant.now());
        when(rekordRepository.findByKorisnikIdAndVezbaIdIn(eq(1L), any())).thenReturn(List.of(heavierWeight, weaker1rm));

        List<LicniRekord> changed = service.evaluate(trening);

        assertThat(find(changed, "Bench Press", TipRekorda.MAX_WEIGHT)).isNull();
        assertThat(heavierWeight.getKilaza()).isEqualByComparingTo("80");
        assertThat(find(changed, "Bench Press", TipRekorda.ESTIMATED_1RM)).isSameAs(weaker1rm);
        assertThat(weaker1rm.getEstimated1rm()).isEqualByComparingTo("84.00");
    }
}
