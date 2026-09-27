package com.fitpulse.backend.workout;

import com.fitpulse.backend.exercise.MuscleGroup;
import com.fitpulse.backend.exercise.Exercise;
import com.fitpulse.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class WorkoutExerciseTest {

    private WorkoutExercise exercise;

    @BeforeEach
    void setUp() {
        Workout workout = Workout.start(User.register("Pera", "Perić", "pera@example.com", "hash", null), null);
        exercise = workout.addExercise(Exercise.create("Bench Press", MuscleGroup.CHEST, null, null));
    }

    @Test
    void addSet_shouldIncreaseSetCountAndNumberSetsInOrder() {
        exercise.addSet(8, new BigDecimal("60"));
        exercise.addSet(8, new BigDecimal("60"));

        assertThat(exercise.getSetCount()).isEqualTo(2);
        assertThat(exercise.getSets()).extracting(WorkoutSet::getPosition).containsExactly(1, 2);
    }

    @Test
    void updateSet_whenAllSetsCompleted_shouldMarkExerciseCompleted() {
        WorkoutSet first = exercise.addSet(8, new BigDecimal("60"));
        WorkoutSet second = exercise.addSet(8, new BigDecimal("60"));

        exercise.updateSet(first, 8, new BigDecimal("60"), true, null);
        assertThat(exercise.isCompleted()).isFalse();

        exercise.updateSet(second, 6, new BigDecimal("65"), true, 90);
        assertThat(exercise.isCompleted()).isTrue();
        assertThat(second.getWeight()).isEqualByComparingTo("65");
        assertThat(second.getRestAfter()).isEqualTo(90);
    }

    @Test
    void removeSet_shouldRenumberRemainingSetsAndRecalculateCompletion() {
        WorkoutSet first = exercise.addSet(8, null);
        WorkoutSet second = exercise.addSet(8, null);
        WorkoutSet third = exercise.addSet(8, null);
        exercise.updateSet(first, 8, null, true, null);
        exercise.updateSet(third, 8, null, true, null);

        exercise.removeSet(second);

        assertThat(exercise.getSetCount()).isEqualTo(2);
        assertThat(exercise.getSets()).extracting(WorkoutSet::getPosition).containsExactly(1, 2);
        assertThat(exercise.isCompleted()).isTrue();
    }
}
