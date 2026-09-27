package com.fitpulse.backend.template;

import com.fitpulse.backend.common.ApiException;
import com.fitpulse.backend.common.OwnershipGuard;
import com.fitpulse.backend.exercise.MuscleGroup;
import com.fitpulse.backend.exercise.Exercise;
import com.fitpulse.backend.exercise.ExerciseRepository;
import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.template.dto.TemplateRequest;
import com.fitpulse.backend.template.dto.TemplateResponse;
import com.fitpulse.backend.template.dto.TemplateExerciseRequest;
import com.fitpulse.backend.template.dto.TemplateExerciseResponse;
import com.fitpulse.backend.user.User;
import com.fitpulse.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock private TemplateRepository templateRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private UserRepository userRepository;

    private TemplateService templateService;
    private CustomUserDetails owner;
    private Template template;
    private Exercise bench;
    private Exercise squat;

    @BeforeEach
    void setUp() {
        templateService = new TemplateService(templateRepository, exerciseRepository, userRepository, new OwnershipGuard());

        User user = User.register("Pera", "Perić", "pera@example.com", "hash", null);
        user.setId(1L);
        owner = new CustomUserDetails(user);

        bench = exercise(10L, "Bench Press", null);
        squat = exercise(11L, "Squat", null);

        template = Template.create("Push", "stari opis", user);
        template.setId(5L);
        template.addExercise(bench, 3, 8, new BigDecimal("60"));
    }

    private static Exercise exercise(Long id, String name, User createdBy) {
        Exercise exercise = Exercise.create(name, MuscleGroup.CHEST, null, createdBy);
        exercise.setId(id);
        return exercise;
    }

    @Test
    void update_withoutExercises_shouldChangeOnlyNameAndDescription() {
        when(templateRepository.findWithExercisesById(5L)).thenReturn(Optional.of(template));

        TemplateResponse response = templateService.update(5L, new TemplateRequest("Push A", "novi opis", null), owner);

        assertThat(response.name()).isEqualTo("Push A");
        assertThat(response.description()).isEqualTo("novi opis");
        assertThat(response.exercises()).extracting(TemplateExerciseResponse::exerciseName).containsExactly("Bench Press");
        verify(templateRepository, never()).flush();
    }

    @Test
    void update_withExercises_shouldReplaceStructureInGivenOrder() {
        when(templateRepository.findWithExercisesById(5L)).thenReturn(Optional.of(template));
        when(exerciseRepository.findById(11L)).thenReturn(Optional.of(squat));
        when(exerciseRepository.findById(10L)).thenReturn(Optional.of(bench));

        List<TemplateExerciseRequest> newStructure = List.of(
                new TemplateExerciseRequest(11L, 5, 5, new BigDecimal("100")),
                new TemplateExerciseRequest(10L, 4, 10, null));

        TemplateResponse response = templateService.update(5L, new TemplateRequest("Push", null, newStructure), owner);

        assertThat(response.exercises())
                .extracting(TemplateExerciseResponse::exerciseName, TemplateExerciseResponse::position)
                .containsExactly(tuple("Squat", 1), tuple("Bench Press", 2));
    }

    @Test
    void update_withExerciseOwnedByAnotherUser_shouldThrowBadRequest() {
        User drugi = User.register("Mika", "Mikić", "mika@example.com", "hash", null);
        drugi.setId(2L);
        Exercise tudja = exercise(20L, "Tuđa vežba", drugi);

        when(templateRepository.findWithExercisesById(5L)).thenReturn(Optional.of(template));
        when(exerciseRepository.findById(20L)).thenReturn(Optional.of(tudja));

        TemplateRequest request = new TemplateRequest("Push", null,
                List.of(new TemplateExerciseRequest(20L, 3, 8, null)));

        assertThatThrownBy(() -> templateService.update(5L, request, owner))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("nije dostupna");
    }
}
