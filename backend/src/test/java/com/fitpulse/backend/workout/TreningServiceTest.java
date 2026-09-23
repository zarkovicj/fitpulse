package com.fitpulse.backend.workout;

import com.fitpulse.backend.common.ApiException;
import com.fitpulse.backend.common.OwnershipGuard;
import com.fitpulse.backend.exercise.MisicnaGrupa;
import com.fitpulse.backend.exercise.Vezba;
import com.fitpulse.backend.exercise.VezbaRepository;
import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.template.Template;
import com.fitpulse.backend.template.TemplateRepository;
import com.fitpulse.backend.template.TemplateVezba;
import com.fitpulse.backend.user.Korisnik;
import com.fitpulse.backend.user.KorisnikRepository;
import com.fitpulse.backend.workout.dto.FinishTreningRequest;
import com.fitpulse.backend.workout.dto.StartTreningRequest;
import com.fitpulse.backend.workout.dto.TreningResponse;
import com.fitpulse.backend.workout.dto.TreningSerijaResponse;
import com.fitpulse.backend.workout.dto.TreningVezbaRequest;
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
class TreningServiceTest {

    @Mock private TreningRepository treningRepository;
    @Mock private TreningVezbaRepository treningVezbaRepository;
    @Mock private TemplateRepository templateRepository;
    @Mock private VezbaRepository vezbaRepository;
    @Mock private KorisnikRepository korisnikRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private TreningService treningService;
    private Korisnik korisnik;
    private CustomUserDetails principal;
    private Vezba bench;
    private Template template;

    @BeforeEach
    void setUp() {
        treningService = new TreningService(treningRepository, treningVezbaRepository, templateRepository,
                vezbaRepository, korisnikRepository, new OwnershipGuard(), eventPublisher);

        korisnik = Korisnik.register("Pera", "Perić", "pera@example.com", "hash", null);
        korisnik.setId(1L);
        principal = new CustomUserDetails(korisnik);

        bench = Vezba.create("Bench Press", MisicnaGrupa.GRUDI, null, null, null);
        bench.setId(10L);

        template = Template.create("Push", null, korisnik);
        template.setId(5L);
        template.addExercise(bench, 3, 8, new BigDecimal("50"));
    }

    private void stubStartFromTemplate(List<TreningVezba> history) {
        when(treningRepository.existsByKorisnikIdAndStatus(1L, StatusTreninga.IN_PROGRESS)).thenReturn(false);
        when(templateRepository.findWithExercisesById(5L)).thenReturn(Optional.of(template));
        when(korisnikRepository.getReferenceById(1L)).thenReturn(korisnik);
        when(treningVezbaRepository.findLatestForUser(eq(1L), eq(10L), eq(StatusTreninga.COMPLETED), any()))
                .thenReturn(history);
        when(treningRepository.save(any(Trening.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void start_fromTemplateWithoutHistory_shouldUseTemplateValues() {
        stubStartFromTemplate(List.of());

        TreningResponse response = treningService.start(new StartTreningRequest(5L, null), principal);

        assertThat(response.exercises()).singleElement().satisfies(exercise ->
                assertThat(exercise.sets())
                        .extracting(TreningSerijaResponse::brojPonavljanja, TreningSerijaResponse::kilaza)
                        .containsExactly(
                                tuple(8, new BigDecimal("50")),
                                tuple(8, new BigDecimal("50")),
                                tuple(8, new BigDecimal("50"))));
    }

    @Test
    void start_fromTemplateWithHistory_shouldCopyValuesFromLastWorkout() {
        Trening previous = Trening.start(korisnik, template);
        TreningVezba previousBench = previous.addExercise(bench);
        previousBench.addSet(8, new BigDecimal("60"));
        previousBench.addSet(6, new BigDecimal("65"));
        previous.finish();
        stubStartFromTemplate(List.of(previousBench));

        TreningResponse response = treningService.start(new StartTreningRequest(5L, null), principal);

        // template traži 3 serije, a istorija ima 2: treća ponavlja poslednju
        assertThat(response.exercises().getFirst().sets())
                .extracting(TreningSerijaResponse::brojPonavljanja, TreningSerijaResponse::kilaza)
                .containsExactly(
                        tuple(8, new BigDecimal("60")),
                        tuple(6, new BigDecimal("65")),
                        tuple(6, new BigDecimal("65")));
    }

    @Test
    void start_whenWorkoutAlreadyInProgress_shouldThrowConflict() {
        when(treningRepository.existsByKorisnikIdAndStatus(1L, StatusTreninga.IN_PROGRESS)).thenReturn(true);

        assertThatThrownBy(() -> treningService.start(new StartTreningRequest(5L, null), principal))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus().value())
                .isEqualTo(409);
    }

    @Test
    void start_withBothTemplateAndExercises_shouldThrowBadRequest() {
        StartTreningRequest request = new StartTreningRequest(5L, List.of(new TreningVezbaRequest(10L, null, null, null)));

        assertThatThrownBy(() -> treningService.start(request, principal))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus().value())
                .isEqualTo(400);
    }

    @Test
    void finish_withUpdateTemplate_shouldCopyLastCompletedSetIntoTemplate() {
        Trening trening = Trening.start(korisnik, template);
        TreningVezba exercise = trening.addExercise(bench);
        TreningSerija first = exercise.addSet(8, new BigDecimal("50"));
        TreningSerija second = exercise.addSet(8, new BigDecimal("50"));
        exercise.addSet(8, new BigDecimal("50"));
        exercise.addSet(8, new BigDecimal("50"));
        exercise.updateSet(first, 10, new BigDecimal("55"), true, null);
        exercise.updateSet(second, 8, new BigDecimal("60"), true, null);
        when(treningRepository.findWithExercisesByIdAndKorisnikId(7L, 1L)).thenReturn(Optional.of(trening));

        TreningResponse response = treningService.finish(7L, new FinishTreningRequest(true), principal);

        TemplateVezba updated = template.getExercises().getFirst();
        assertThat(response.status()).isEqualTo(StatusTreninga.COMPLETED);
        assertThat(updated.getBrojSerija()).isEqualTo(4);
        assertThat(updated.getBrojPonavljanja()).isEqualTo(8);
        assertThat(updated.getKilaza()).isEqualByComparingTo("60");
        verify(eventPublisher).publishEvent(any(TreningZavrsenEvent.class));
    }

    @Test
    void finish_withUpdateTemplateOnSystemTemplate_shouldThrowForbiddenAndKeepWorkoutInProgress() {
        Template systemTemplate = Template.create("Sistemski", null, null);
        Trening trening = Trening.start(korisnik, systemTemplate);
        when(treningRepository.findWithExercisesByIdAndKorisnikId(7L, 1L)).thenReturn(Optional.of(trening));

        assertThatThrownBy(() -> treningService.finish(7L, new FinishTreningRequest(true), principal))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus().value())
                .isEqualTo(403);
        assertThat(trening.isInProgress()).isTrue();
        verify(eventPublisher, never()).publishEvent(any());
    }
}
