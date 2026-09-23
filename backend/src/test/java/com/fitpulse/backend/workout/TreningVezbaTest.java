package com.fitpulse.backend.workout;

import com.fitpulse.backend.exercise.MisicnaGrupa;
import com.fitpulse.backend.exercise.Vezba;
import com.fitpulse.backend.user.Korisnik;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TreningVezbaTest {

    private TreningVezba exercise;

    @BeforeEach
    void setUp() {
        Trening trening = Trening.start(Korisnik.register("Pera", "Perić", "pera@example.com", "hash", null), null);
        exercise = trening.addExercise(Vezba.create("Bench Press", MisicnaGrupa.GRUDI, null, null, null));
    }

    @Test
    void addSet_shouldIncreaseSetCountAndNumberSetsInOrder() {
        exercise.addSet(8, new BigDecimal("60"));
        exercise.addSet(8, new BigDecimal("60"));

        assertThat(exercise.getBrojSerija()).isEqualTo(2);
        assertThat(exercise.getSets()).extracting(TreningSerija::getRedniBroj).containsExactly(1, 2);
    }

    @Test
    void updateSet_whenAllSetsCompleted_shouldMarkExerciseCompleted() {
        TreningSerija first = exercise.addSet(8, new BigDecimal("60"));
        TreningSerija second = exercise.addSet(8, new BigDecimal("60"));

        exercise.updateSet(first, 8, new BigDecimal("60"), true, null);
        assertThat(exercise.isCompleted()).isFalse();

        exercise.updateSet(second, 6, new BigDecimal("65"), true, 90);
        assertThat(exercise.isCompleted()).isTrue();
        assertThat(second.getKilaza()).isEqualByComparingTo("65");
        assertThat(second.getRestAfter()).isEqualTo(90);
    }

    @Test
    void removeSet_shouldRenumberRemainingSetsAndRecalculateCompletion() {
        TreningSerija first = exercise.addSet(8, null);
        TreningSerija second = exercise.addSet(8, null);
        TreningSerija third = exercise.addSet(8, null);
        exercise.updateSet(first, 8, null, true, null);
        exercise.updateSet(third, 8, null, true, null);

        exercise.removeSet(second);

        assertThat(exercise.getBrojSerija()).isEqualTo(2);
        assertThat(exercise.getSets()).extracting(TreningSerija::getRedniBroj).containsExactly(1, 2);
        assertThat(exercise.isCompleted()).isTrue();
    }
}
