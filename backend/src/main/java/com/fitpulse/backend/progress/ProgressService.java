package com.fitpulse.backend.progress;

import com.fitpulse.backend.common.ApiException;
import com.fitpulse.backend.common.AppTime;
import com.fitpulse.backend.common.OwnershipGuard;
import com.fitpulse.backend.exercise.ExerciseRepository;
import com.fitpulse.backend.progress.dto.*;
import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.user.User;
import com.fitpulse.backend.user.UserRepository;
import com.fitpulse.backend.workout.WorkoutStatus;
import com.fitpulse.backend.workout.WorkoutRepository;
import com.fitpulse.backend.workout.WorkoutSet;
import com.fitpulse.backend.workout.WorkoutSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static com.fitpulse.backend.progress.RecordCalculator.canEstimate1rm;
import static com.fitpulse.backend.progress.RecordCalculator.epley;
import static com.fitpulse.backend.progress.RecordCalculator.hasWeight;

@Service
public class ProgressService {

    private final BodyGoalRepository bodyGoalRepository;
    private final BodyWeightLogRepository weightLogRepository;
    private final PersonalRecordRepository recordRepository;
    private final WorkoutRepository workoutRepository;
    private final WorkoutSetRepository workoutSetRepository;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final OwnershipGuard ownershipGuard;

    public ProgressService(BodyGoalRepository bodyGoalRepository,
                           BodyWeightLogRepository weightLogRepository,
                           PersonalRecordRepository recordRepository,
                           WorkoutRepository workoutRepository,
                           WorkoutSetRepository workoutSetRepository,
                           UserRepository userRepository,
                           ExerciseRepository exerciseRepository,
                           OwnershipGuard ownershipGuard) {
        this.bodyGoalRepository = bodyGoalRepository;
        this.weightLogRepository = weightLogRepository;
        this.recordRepository = recordRepository;
        this.workoutRepository = workoutRepository;
        this.workoutSetRepository = workoutSetRepository;
        this.userRepository = userRepository;
        this.exerciseRepository = exerciseRepository;
        this.ownershipGuard = ownershipGuard;
    }

    @Transactional(readOnly = true)
    public BodyGoalResponse getGoal(CustomUserDetails principal) {
        return bodyGoalRepository.findByUserId(principal.getId())
                .map(goal -> new BodyGoalResponse(goal.getWeight(), goal.getBodyFatPercent()))
                .orElse(new BodyGoalResponse(null, null));
    }

    @Transactional
    public BodyGoalResponse updateGoal(BodyGoalRequest request, CustomUserDetails principal) {
        BodyGoal goal = bodyGoalRepository.findByUserId(principal.getId())
                .orElseGet(() -> BodyGoal.create(userRepository.getReferenceById(principal.getId())));

        goal.setWeight(request.weight());
        goal.setBodyFatPercent(request.bodyFatPercent());
        bodyGoalRepository.save(goal);

        return new BodyGoalResponse(goal.getWeight(), goal.getBodyFatPercent());
    }

    // podrazumevano poslednjih godinu dana
    @Transactional(readOnly = true)
    public List<WeightLogResponse> weightHistory(LocalDate from, LocalDate to, CustomUserDetails principal) {
        LocalDate end = to != null ? to : AppTime.today();
        LocalDate start = from != null ? from : end.minusYears(1);
        if (start.isAfter(end)) {
            throw ApiException.badRequest("Početni datum mora biti pre krajnjeg");
        }
        return weightLogRepository.findByUserIdAndDateBetweenOrderByDate(principal.getId(), start, end).stream()
                .map(WeightLogResponse::from)
                .toList();
    }

    // jedno merenje po danu: ponovni unos za isti dan menja postojece
    @Transactional
    public WeightLogResponse logWeight(LocalDate date, WeightRequest request, CustomUserDetails principal) {
        if (date.isAfter(AppTime.today())) {
            throw ApiException.badRequest("Merenje ne može biti u budućnosti");
        }

        Long userId = principal.getId();
        BodyWeightLog log = weightLogRepository.findByUserIdAndDate(userId, date)
                .orElseGet(() -> BodyWeightLog.create(userRepository.getReferenceById(userId), date, request.weight()));
        log.setWeight(request.weight());
        weightLogRepository.save(log);

        syncCurrentWeight(userId);
        return WeightLogResponse.from(log);
    }

    @Transactional
    public void deleteWeight(LocalDate date, CustomUserDetails principal) {
        Long userId = principal.getId();
        BodyWeightLog log = weightLogRepository.findByUserIdAndDate(userId, date)
                .orElseThrow(() -> ApiException.notFound("Merenje za taj dan ne postoji"));
        weightLogRepository.delete(log);
        syncCurrentWeight(userId);
    }

    @Transactional(readOnly = true)
    public ProgressSummaryResponse summary(CustomUserDetails principal) {
        Long userId = principal.getId();
        LocalDate today = AppTime.today();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        BigDecimal volume = workoutSetRepository.sumVolumeSince(userId, WorkoutStatus.COMPLETED, today.minusDays(30));
        Optional<BodyGoal> goal = bodyGoalRepository.findByUserId(userId);

        return new ProgressSummaryResponse(
                workoutRepository.countByUserIdAndStatus(userId, WorkoutStatus.COMPLETED),
                workoutRepository.countByUserIdAndStatusAndDateGreaterThanEqual(userId, WorkoutStatus.COMPLETED, monday),
                volume != null ? volume : BigDecimal.ZERO,
                recordRepository.countByUserId(userId),
                userRepository.findById(userId).map(User::getWeight).orElse(null),
                goal.map(BodyGoal::getWeight).orElse(null),
                goal.map(BodyGoal::getBodyFatPercent).orElse(null)
        );
    }

    @Transactional(readOnly = true)
    public List<ExerciseProgressResponse> exerciseHistory(Long exerciseId, CustomUserDetails principal) {
        exerciseRepository.findById(exerciseId)
                .filter(exercise -> ownershipGuard.canView(exercise.getOwnerId(), principal))
                .orElseThrow(() -> ApiException.notFound("Vežba nije pronađena"));

        Map<Long, List<WorkoutSet>> setsByWorkout = new LinkedHashMap<>();
        workoutSetRepository.findCompletedSetsForExercise(principal.getId(), exerciseId, WorkoutStatus.COMPLETED)
                .forEach(set -> setsByWorkout
                        .computeIfAbsent(set.getWorkoutExercise().getWorkout().getId(), id -> new ArrayList<>())
                        .add(set));

        return setsByWorkout.values().stream().map(ProgressService::toPoint).toList();
    }

    private static ExerciseProgressResponse toPoint(List<WorkoutSet> sets) {
        List<WorkoutSet> weighted = sets.stream().filter(set -> hasWeight(set.getWeight())).toList();

        return new ExerciseProgressResponse(
                sets.getFirst().getWorkoutExercise().getWorkout().getId(),
                sets.getFirst().getWorkoutExercise().getWorkout().getDate(),
                weighted.stream().map(WorkoutSet::getWeight).max(Comparator.naturalOrder()).orElse(null),
                weighted.stream()
                        .filter(set -> canEstimate1rm(set.getWeight(), set.getReps()))
                        .map(set -> epley(set.getWeight(), set.getReps()))
                        .max(Comparator.naturalOrder()).orElse(null),
                sets.stream().mapToInt(WorkoutSet::getReps).max().orElse(0),
                weighted.stream().map(set -> set.getWeight().multiply(BigDecimal.valueOf(set.getReps())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    private void syncCurrentWeight(Long userId) {
        User user = userRepository.getReferenceById(userId);
        user.setWeight(weightLogRepository.findFirstByUserIdOrderByDateDesc(userId)
                .map(BodyWeightLog::getWeight)
                .orElse(null));
    }
}
