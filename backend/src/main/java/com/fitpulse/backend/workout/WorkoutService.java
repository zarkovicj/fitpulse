package com.fitpulse.backend.workout;

import com.fitpulse.backend.common.ApiException;
import com.fitpulse.backend.common.OwnershipGuard;
import com.fitpulse.backend.common.PageResponse;
import com.fitpulse.backend.exercise.Exercise;
import com.fitpulse.backend.exercise.ExerciseRepository;
import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.template.Template;
import com.fitpulse.backend.template.TemplateRepository;
import com.fitpulse.backend.template.TemplateExercise;
import com.fitpulse.backend.user.UserRepository;
import com.fitpulse.backend.workout.dto.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WorkoutService {

    private static final int DEFAULT_SET_COUNT = 3;
    private static final int DEFAULT_REPS = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_EXERCISES = 30;
    private static final int MAX_SETS = 20;

    private final WorkoutRepository workoutRepository;
    private final WorkoutExerciseRepository workoutExerciseRepository;
    private final TemplateRepository templateRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final OwnershipGuard ownershipGuard;
    private final ApplicationEventPublisher eventPublisher;

    public WorkoutService(WorkoutRepository workoutRepository,
                          WorkoutExerciseRepository workoutExerciseRepository,
                          TemplateRepository templateRepository,
                          ExerciseRepository exerciseRepository,
                          UserRepository userRepository,
                          OwnershipGuard ownershipGuard,
                          ApplicationEventPublisher eventPublisher) {
        this.workoutRepository = workoutRepository;
        this.workoutExerciseRepository = workoutExerciseRepository;
        this.templateRepository = templateRepository;
        this.exerciseRepository = exerciseRepository;
        this.userRepository = userRepository;
        this.ownershipGuard = ownershipGuard;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public WorkoutResponse start(StartWorkoutRequest request, CustomUserDetails principal) {
        Long userId = principal.getId();
        boolean hasExercises = request.exercises() != null && !request.exercises().isEmpty();

        if (request.templateId() != null && hasExercises) {
            throw ApiException.badRequest("Trening se pokreće ili iz template-a ili sa listom vežbi, ne oba");
        }
        if (workoutRepository.existsByUserIdAndStatus(userId, WorkoutStatus.IN_PROGRESS)) {
            throw ApiException.conflict("Već imate trening u toku");
        }

        Template template = request.templateId() != null ? findUsableTemplate(request.templateId(), principal) : null;
        Workout workout = Workout.start(userRepository.getReferenceById(userId), template);

        if (template != null) {
            for (TemplateExercise item : template.getExercises()) {
                WorkoutExercise exercise = workout.addExercise(item.getExercise());
                prefillSets(exercise, userId, item.getSetCount(), item.getReps(), item.getWeight());
            }
        } else if (hasExercises) {
            request.exercises().forEach(item -> addExerciseFromRequest(workout, item, userId));
        }

        return WorkoutResponse.from(workoutRepository.save(workout));
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutResponse> getActive(CustomUserDetails principal) {
        return workoutRepository.findFirstByUserIdAndStatus(principal.getId(), WorkoutStatus.IN_PROGRESS)
                .map(WorkoutResponse::from);
    }

    @Transactional(readOnly = true)
    public WorkoutResponse getById(Long id, CustomUserDetails principal) {
        return WorkoutResponse.from(findOwnedOrThrow(id, principal));
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkoutSummaryResponse> history(int page, int size, CustomUserDetails principal) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        return PageResponse.from(workoutRepository.findHistory(principal.getId(), pageRequest)
                .map(WorkoutSummaryResponse::from));
    }

    @Transactional
    public WorkoutExerciseResponse addExercise(Long id, WorkoutExerciseRequest request, CustomUserDetails principal) {
        Workout workout = findInProgressOrThrow(id, principal);
        if (workout.getExercises().size() >= MAX_EXERCISES) {
            throw ApiException.badRequest("Trening može imati najviše " + MAX_EXERCISES + " vežbi");
        }
        WorkoutExercise exercise = addExerciseFromRequest(workout, request, principal.getId());
        workoutRepository.flush();
        return WorkoutExerciseResponse.from(exercise);
    }

    @Transactional
    public void removeExercise(Long id, Long exerciseId, CustomUserDetails principal) {
        Workout workout = findInProgressOrThrow(id, principal);
        workout.removeExercise(findExerciseOrThrow(workout, exerciseId));
    }

    // nova serija kopira vrednosti poslednje i odmor, da korisnik ne kuca isto ponovo
    @Transactional
    public WorkoutSetResponse addSet(Long id, Long exerciseId, CustomUserDetails principal) {
        WorkoutExercise exercise = findExerciseOrThrow(findInProgressOrThrow(id, principal), exerciseId);
        List<WorkoutSet> sets = exercise.getSets();
        if (sets.size() >= MAX_SETS) {
            throw ApiException.badRequest("Vežba može imati najviše " + MAX_SETS + " serija");
        }

        WorkoutSet set = sets.isEmpty()
                ? exercise.addSet(DEFAULT_REPS, null)
                : exercise.addSet(sets.getLast().getReps(), sets.getLast().getWeight(), sets.getLast().getRestAfter());

        workoutRepository.flush();
        return WorkoutSetResponse.from(set);
    }

    @Transactional
    public WorkoutSetResponse updateSet(Long id, Long exerciseId, Long setId, WorkoutSetRequest request,
                                           CustomUserDetails principal) {
        WorkoutExercise exercise = findExerciseOrThrow(findInProgressOrThrow(id, principal), exerciseId);
        WorkoutSet set = findSetOrThrow(exercise, setId);

        exercise.updateSet(set, request.reps(), request.weight(), request.completed(), request.restAfter());
        return WorkoutSetResponse.from(set);
    }

    @Transactional
    public void removeSet(Long id, Long exerciseId, Long setId, CustomUserDetails principal) {
        WorkoutExercise exercise = findExerciseOrThrow(findInProgressOrThrow(id, principal), exerciseId);
        exercise.removeSet(findSetOrThrow(exercise, setId));
    }

    @Transactional
    public WorkoutResponse finish(Long id, FinishWorkoutRequest request, CustomUserDetails principal) {
        Workout workout = findInProgressOrThrow(id, principal);

        if (request != null && request.updateTemplate()) {
            Template template = workout.getTemplate();
            if (template == null) {
                throw ApiException.badRequest("Trening nije pokrenut iz template-a");
            }
            ownershipGuard.assertCanModify(template.getOwnerId(), principal);
            applyValuesToTemplate(workout, template);
        }

        workout.finish();
        eventPublisher.publishEvent(new WorkoutFinishedEvent(workout.getId()));
        return WorkoutResponse.from(workout);
    }

    @Transactional
    public WorkoutResponse cancel(Long id, CustomUserDetails principal) {
        Workout workout = findInProgressOrThrow(id, principal);
        workout.cancel();
        return WorkoutResponse.from(workout);
    }

    // broj serija iz template: ponavljanja, kilaza i odmor iz poslednjeg zavrsenog treninga
    private void prefillSets(WorkoutExercise exercise, Long userId, int setCount, int reps, BigDecimal weight) {
        List<WorkoutSet> previous = workoutExerciseRepository
                .findLatestForUser(userId, exercise.getExercise().getId(), WorkoutStatus.COMPLETED, Limit.of(1))
                .stream()
                .findFirst()
                .map(WorkoutExercise::getSets)
                .orElse(List.of());

        for (int i = 0; i < setCount; i++) {
            if (previous.isEmpty()) {
                exercise.addSet(reps, weight);
            } else {
                WorkoutSet source = previous.get(Math.min(i, previous.size() - 1));
                exercise.addSet(source.getReps(), source.getWeight(), source.getRestAfter());
            }
        }
    }

    private WorkoutExercise addExerciseFromRequest(Workout workout, WorkoutExerciseRequest item, Long userId) {
        WorkoutExercise exercise = workout.addExercise(findUsableExercise(item.exerciseId(), userId));
        prefillSets(exercise, userId,
                item.setCount() != null ? item.setCount() : DEFAULT_SET_COUNT,
                item.reps() != null ? item.reps() : DEFAULT_REPS,
                item.weight());
        return exercise;
    }

    private void applyValuesToTemplate(Workout workout, Template template) {
        List<WorkoutExercise> remaining = new ArrayList<>(workout.getExercises());

        for (TemplateExercise item : template.getExercises()) {
            Optional<WorkoutExercise> match = remaining.stream()
                    .filter(exercise -> exercise.getExercise().getId().equals(item.getExercise().getId()))
                    .findFirst();
            if (match.isEmpty()) {
                continue;
            }
            remaining.remove(match.get());

            List<WorkoutSet> sets = match.get().getSets();
            // serija sa 0 ponavljanja se preskace, mora imati bar 1 ponavljanje
            sets.stream()
                    .filter(set -> set.isCompleted() && set.getReps() > 0)
                    .reduce((first, second) -> second)
                    .ifPresent(lastCompleted -> {
                        item.setSetCount(sets.size());
                        item.setReps(lastCompleted.getReps());
                        item.setWeight(lastCompleted.getWeight());
                    });
        }
    }

    private Template findUsableTemplate(Long templateId, CustomUserDetails principal) {
        return templateRepository.findWithExercisesById(templateId)
                .filter(template -> ownershipGuard.canView(template.getOwnerId(), principal))
                .orElseThrow(() -> ApiException.notFound("Template nije pronađen"));
    }

    private Exercise findUsableExercise(Long exerciseId, Long userId) {
        return exerciseRepository.findById(exerciseId)
                .filter(exercise -> exercise.getOwnerId() == null || exercise.getOwnerId().equals(userId))
                .orElseThrow(() -> ApiException.badRequest("Vežba " + exerciseId + " nije dostupna"));
    }

    private Workout findOwnedOrThrow(Long id, CustomUserDetails principal) {
        return workoutRepository.findWithExercisesByIdAndUserId(id, principal.getId())
                .orElseThrow(() -> ApiException.notFound("Trening nije pronađen"));
    }

    private Workout findInProgressOrThrow(Long id, CustomUserDetails principal) {
        Workout workout = findOwnedOrThrow(id, principal);
        if (!workout.isInProgress()) {
            throw ApiException.conflict("Trening nije u toku");
        }
        return workout;
    }

    private WorkoutExercise findExerciseOrThrow(Workout workout, Long exerciseId) {
        return workout.getExercises().stream()
                .filter(exercise -> exercise.getId().equals(exerciseId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Vežba nije pronađena u treningu"));
    }

    private WorkoutSet findSetOrThrow(WorkoutExercise exercise, Long setId) {
        return exercise.getSets().stream()
                .filter(set -> set.getId().equals(setId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Serija nije pronađena"));
    }
}
