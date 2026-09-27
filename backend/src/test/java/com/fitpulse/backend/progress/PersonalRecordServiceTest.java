package com.fitpulse.backend.progress;

import com.fitpulse.backend.exercise.MuscleGroup;
import com.fitpulse.backend.exercise.Exercise;
import com.fitpulse.backend.user.User;
import com.fitpulse.backend.workout.Workout;
import com.fitpulse.backend.workout.WorkoutRepository;
import com.fitpulse.backend.workout.WorkoutSet;
import com.fitpulse.backend.workout.WorkoutExercise;
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
class PersonalRecordServiceTest {

    @Mock private PersonalRecordRepository recordRepository;
    @Mock private WorkoutRepository workoutRepository;

    private PersonalRecordService service;
    private User user;
    private Exercise bench;
    private Workout workout;

    @BeforeEach
    void setUp() {
        service = new PersonalRecordService(recordRepository, workoutRepository);

        user = User.register("Pera", "Perić", "pera@example.com", "hash", null);
        user.setId(1L);
        bench = Exercise.create("Bench Press", MuscleGroup.CHEST, null, null);
        bench.setId(10L);
        Exercise pullUpsExercise = Exercise.create("Pull-ups", MuscleGroup.BACK, null, null);
        pullUpsExercise.setId(11L);

        workout = Workout.start(user, null);
        WorkoutExercise benchExercise = workout.addExercise(bench);
        complete(benchExercise, benchExercise.addSet(8, new BigDecimal("60")));
        complete(benchExercise, benchExercise.addSet(6, new BigDecimal("70")));
        benchExercise.addSet(12, new BigDecimal("40"));
        WorkoutExercise pullUps = workout.addExercise(pullUpsExercise);
        complete(pullUps, pullUps.addSet(12, null));
        workout.finish();

        when(recordRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static void complete(WorkoutExercise exercise, WorkoutSet set) {
        exercise.updateSet(set, set.getReps(), set.getWeight(), true, null);
    }

    private static PersonalRecord find(List<PersonalRecord> records, String exercise, RecordType type) {
        return records.stream()
                .filter(r -> r.getExercise().getName().equals(exercise) && r.getType() == type)
                .findFirst().orElse(null);
    }

    @Test
    void evaluate_withoutExistingRecords_shouldCreateRecordsFromCompletedSetsOnly() {
        when(recordRepository.findByUserIdAndExerciseIdIn(eq(1L), any())).thenReturn(List.of());

        List<PersonalRecord> records = service.evaluate(workout);

        assertThat(records).hasSize(4);
        assertThat(find(records, "Bench Press", RecordType.MAX_WEIGHT).getWeight()).isEqualByComparingTo("70");
        assertThat(find(records, "Bench Press", RecordType.ESTIMATED_1RM).getEstimated1rm()).isEqualByComparingTo("84.00");
        assertThat(find(records, "Bench Press", RecordType.MAX_REPS).getReps()).isEqualTo(8);
        PersonalRecord pullUpReps = find(records, "Pull-ups", RecordType.MAX_REPS);
        assertThat(pullUpReps.getReps()).isEqualTo(12);
        assertThat(pullUpReps.getWeight()).isNull();
        assertThat(find(records, "Pull-ups", RecordType.MAX_WEIGHT)).isNull();
    }

    @Test
    void evaluate_withExistingRecords_shouldUpdateOnlyThoseThatWereBeaten() {
        PersonalRecord heavierWeight = PersonalRecord.create(user, bench, RecordType.MAX_WEIGHT);
        heavierWeight.record(3, new BigDecimal("80"), new BigDecimal("88.00"), 99L, Instant.now());
        PersonalRecord weaker1rm = PersonalRecord.create(user, bench, RecordType.ESTIMATED_1RM);
        weaker1rm.record(5, new BigDecimal("65"), new BigDecimal("75.83"), 99L, Instant.now());
        when(recordRepository.findByUserIdAndExerciseIdIn(eq(1L), any())).thenReturn(List.of(heavierWeight, weaker1rm));

        List<PersonalRecord> changed = service.evaluate(workout);

        assertThat(find(changed, "Bench Press", RecordType.MAX_WEIGHT)).isNull();
        assertThat(heavierWeight.getWeight()).isEqualByComparingTo("80");
        assertThat(find(changed, "Bench Press", RecordType.ESTIMATED_1RM)).isSameAs(weaker1rm);
        assertThat(weaker1rm.getEstimated1rm()).isEqualByComparingTo("84.00");
    }
}
