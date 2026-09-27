package com.fitpulse.backend.progress;

import com.fitpulse.backend.exercise.Exercise;
import com.fitpulse.backend.progress.dto.PersonalRecordResponse;
import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.workout.Workout;
import com.fitpulse.backend.workout.WorkoutRepository;
import com.fitpulse.backend.workout.WorkoutSet;
import com.fitpulse.backend.workout.WorkoutExercise;
import com.fitpulse.backend.workout.WorkoutFinishedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.fitpulse.backend.progress.RecordCalculator.canEstimate1rm;
import static com.fitpulse.backend.progress.RecordCalculator.epley;
import static com.fitpulse.backend.progress.RecordCalculator.hasWeight;
import static com.fitpulse.backend.progress.RecordCalculator.orZero;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;

@Service
public class PersonalRecordService {

    private final PersonalRecordRepository recordRepository;
    private final WorkoutRepository workoutRepository;

    public PersonalRecordService(PersonalRecordRepository recordRepository, WorkoutRepository workoutRepository) {
        this.recordRepository = recordRepository;
        this.workoutRepository = workoutRepository;
    }

    // u istoj transakciji kao finish: ako ovo pukne, ni trening se ne zavrsava
    @EventListener
    @Transactional
    public void onWorkoutFinished(WorkoutFinishedEvent event) {
        workoutRepository.findById(event.workoutId()).ifPresent(this::evaluate);
    }

    @Transactional
    public List<PersonalRecord> evaluate(Workout workout) {
        Map<Long, List<WorkoutSet>> completedSetsByExercise = new LinkedHashMap<>();
        Map<Long, Exercise> exercises = new HashMap<>();

        for (WorkoutExercise exercise : workout.getExercises()) {
            Long exerciseId = exercise.getExercise().getId();
            exercises.put(exerciseId, exercise.getExercise());
            exercise.getSets().stream()
                    .filter(WorkoutSet::isCompleted)
                    .forEach(set -> completedSetsByExercise.computeIfAbsent(exerciseId, id -> new ArrayList<>()).add(set));
        }
        if (completedSetsByExercise.isEmpty()) {
            return List.of();
        }

        Map<RecordKey, PersonalRecord> existing = recordRepository
                .findByUserIdAndExerciseIdIn(workout.getUser().getId(), completedSetsByExercise.keySet())
                .stream()
                .collect(Collectors.toMap(r -> new RecordKey(r.getExercise().getId(), r.getType()), Function.identity()));

        List<PersonalRecord> changed = new ArrayList<>();
        completedSetsByExercise.forEach((exerciseId, sets) -> {
            Exercise exercise = exercises.get(exerciseId);

            sets.stream()
                    .filter(set -> hasWeight(set.getWeight()))
                    .max(comparing(WorkoutSet::getWeight).thenComparingInt(WorkoutSet::getReps))
                    .ifPresent(best -> apply(RecordType.MAX_WEIGHT, best, exercise, workout, existing, changed));

            sets.stream()
                    .filter(set -> canEstimate1rm(set.getWeight(), set.getReps()))
                    .max(comparing(set -> epley(set.getWeight(), set.getReps())))
                    .ifPresent(best -> apply(RecordType.ESTIMATED_1RM, best, exercise, workout, existing, changed));

            sets.stream()
                    .filter(set -> set.getReps() >= 1)
                    .max(comparingInt(WorkoutSet::getReps).thenComparing(set -> orZero(set.getWeight())))
                    .ifPresent(best -> apply(RecordType.MAX_REPS, best, exercise, workout, existing, changed));
        });

        return recordRepository.saveAll(changed);
    }

    @Transactional(readOnly = true)
    public List<PersonalRecordResponse> search(Long exerciseId, Long workoutId, CustomUserDetails principal) {
        return recordRepository.search(principal.getId(), exerciseId, workoutId).stream()
                .map(PersonalRecordResponse::from)
                .toList();
    }

    private void apply(RecordType type, WorkoutSet set, Exercise exercise, Workout workout,
                       Map<RecordKey, PersonalRecord> existing, List<PersonalRecord> changed) {
        BigDecimal e1rm = canEstimate1rm(set.getWeight(), set.getReps())
                ? epley(set.getWeight(), set.getReps()) : null;
        PersonalRecord current = existing.get(new RecordKey(exercise.getId(), type));
        if (!isBetter(type, set, e1rm, current)) {
            return;
        }

        PersonalRecord personalRecord = current != null ? current : PersonalRecord.create(workout.getUser(), exercise, type);
        personalRecord.record(set.getReps(), set.getWeight(), e1rm, workout.getId(), workout.getFinishedAt());
        changed.add(personalRecord);
    }

    private static boolean isBetter(RecordType type, WorkoutSet set, BigDecimal e1rm, PersonalRecord current) {
        if (current == null) {
            return true;
        }
        return switch (type) {
            case MAX_WEIGHT -> {
                int cmp = set.getWeight().compareTo(current.getWeight());
                yield cmp > 0 || (cmp == 0 && set.getReps() > current.getReps());
            }
            case ESTIMATED_1RM -> e1rm.compareTo(current.getEstimated1rm()) > 0;
            case MAX_REPS -> set.getReps() > current.getReps()
                    || (set.getReps() == current.getReps()
                        && orZero(set.getWeight()).compareTo(orZero(current.getWeight())) > 0);
        };
    }

    private record RecordKey(Long exerciseId, RecordType type) {
    }
}
