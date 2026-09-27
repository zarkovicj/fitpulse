package com.fitpulse.backend.workout;

import com.fitpulse.backend.common.ApiException;
import com.fitpulse.backend.common.OwnershipGuard;
import com.fitpulse.backend.exercise.MuscleGroup;
import com.fitpulse.backend.exercise.Exercise;
import com.fitpulse.backend.exercise.ExerciseRepository;
import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.template.Template;
import com.fitpulse.backend.template.TemplateRepository;
import com.fitpulse.backend.template.TemplateExercise;
import com.fitpulse.backend.user.User;
import com.fitpulse.backend.user.UserRepository;
import com.fitpulse.backend.workout.dto.FinishWorkoutRequest;
import com.fitpulse.backend.workout.dto.StartWorkoutRequest;
import com.fitpulse.backend.workout.dto.WorkoutResponse;
import com.fitpulse.backend.workout.dto.WorkoutSetResponse;
import com.fitpulse.backend.workout.dto.WorkoutExerciseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

    @Mock private WorkoutRepository workoutRepository;
    @Mock private WorkoutExerciseRepository workoutExerciseRepository;
    @Mock private TemplateRepository templateRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private WorkoutService workoutService;
    private User user;
    private CustomUserDetails principal;
    private Exercise bench;
    private Template template;

    @BeforeEach
    void setUp() {
        workoutService = new WorkoutService(workoutRepository, workoutExerciseRepository, templateRepository,
                exerciseRepository, userRepository, new OwnershipGuard(), eventPublisher);

        user = User.register("Pera", "Perić", "pera@example.com", "hash", null);
        user.setId(1L);
        principal = new CustomUserDetails(user);

        bench = Exercise.create("Bench Press", MuscleGroup.CHEST, null, null);
        bench.setId(10L);

        template = Template.create("Push", null, user);
        template.setId(5L);
        template.addExercise(bench, 3, 8, new BigDecimal("50"));
    }

    private void stubStartFromTemplate(List<WorkoutExercise> history) {
        when(workoutRepository.existsByUserIdAndStatus(1L, WorkoutStatus.IN_PROGRESS)).thenReturn(false);
        when(templateRepository.findWithExercisesById(5L)).thenReturn(Optional.of(template));
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(workoutExerciseRepository.findLatestForUser(eq(1L), eq(10L), eq(WorkoutStatus.COMPLETED), any()))
                .thenReturn(history);
        when(workoutRepository.save(any(Workout.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void start_fromTemplateWithoutHistory_shouldUseTemplateValues() {
        stubStartFromTemplate(List.of());

        WorkoutResponse response = workoutService.start(new StartWorkoutRequest(5L, null), principal);

        assertThat(response.exercises()).singleElement().satisfies(exercise ->
                assertThat(exercise.sets())
                        .extracting(WorkoutSetResponse::reps, WorkoutSetResponse::weight)
                        .containsExactly(
                                tuple(8, new BigDecimal("50")),
                                tuple(8, new BigDecimal("50")),
                                tuple(8, new BigDecimal("50"))));
    }

    @Test
    void start_fromTemplateWithHistory_shouldCopyValuesFromLastWorkout() {
        Workout previous = Workout.start(user, template);
        WorkoutExercise previousBench = previous.addExercise(bench);
        previousBench.addSet(8, new BigDecimal("60"), 90);
        previousBench.addSet(6, new BigDecimal("65"), 120);
        previous.finish();
        stubStartFromTemplate(List.of(previousBench));

        WorkoutResponse response = workoutService.start(new StartWorkoutRequest(5L, null), principal);

        assertThat(response.exercises().getFirst().sets())
                .extracting(WorkoutSetResponse::reps, WorkoutSetResponse::weight, WorkoutSetResponse::restAfter)
                .containsExactly(
                        tuple(8, new BigDecimal("60"), 90),
                        tuple(6, new BigDecimal("65"), 120),
                        tuple(6, new BigDecimal("65"), 120));
    }

    @Test
    void start_whenWorkoutAlreadyInProgress_shouldThrowConflict() {
        when(workoutRepository.existsByUserIdAndStatus(1L, WorkoutStatus.IN_PROGRESS)).thenReturn(true);

        assertThatThrownBy(() -> workoutService.start(new StartWorkoutRequest(5L, null), principal))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus().value())
                .isEqualTo(409);
    }

    @Test
    void start_withBothTemplateAndExercises_shouldThrowBadRequest() {
        StartWorkoutRequest request = new StartWorkoutRequest(5L, List.of(new WorkoutExerciseRequest(10L, null, null, null)));

        assertThatThrownBy(() -> workoutService.start(request, principal))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus().value())
                .isEqualTo(400);
    }

    @Test
    void finish_withUpdateTemplate_shouldCopyLastCompletedSetIntoTemplate() {
        Workout workout = Workout.start(user, template);
        WorkoutExercise exercise = workout.addExercise(bench);
        WorkoutSet first = exercise.addSet(8, new BigDecimal("50"));
        WorkoutSet second = exercise.addSet(8, new BigDecimal("50"));
        exercise.addSet(8, new BigDecimal("50"));
        exercise.addSet(8, new BigDecimal("50"));
        exercise.updateSet(first, 10, new BigDecimal("55"), true, null);
        exercise.updateSet(second, 8, new BigDecimal("60"), true, null);
        when(workoutRepository.findWithExercisesByIdAndUserId(7L, 1L)).thenReturn(Optional.of(workout));

        WorkoutResponse response = workoutService.finish(7L, new FinishWorkoutRequest(true), principal);

        TemplateExercise updated = template.getExercises().getFirst();
        assertThat(response.status()).isEqualTo(WorkoutStatus.COMPLETED);
        assertThat(updated.getSetCount()).isEqualTo(4);
        assertThat(updated.getReps()).isEqualTo(8);
        assertThat(updated.getWeight()).isEqualByComparingTo("60");
        verify(eventPublisher).publishEvent(any(WorkoutFinishedEvent.class));
    }

    @Test
    void finish_withUpdateTemplateOnSystemTemplate_shouldThrowForbiddenAndKeepWorkoutInProgress() {
        Template systemTemplate = Template.create("Sistemski", null, null);
        Workout workout = Workout.start(user, systemTemplate);
        when(workoutRepository.findWithExercisesByIdAndUserId(7L, 1L)).thenReturn(Optional.of(workout));

        assertThatThrownBy(() -> workoutService.finish(7L, new FinishWorkoutRequest(true), principal))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus().value())
                .isEqualTo(403);
        assertThat(workout.isInProgress()).isTrue();
        verify(eventPublisher, never()).publishEvent(any());
    }
}
