package com.fitpulse.backend.template;

import com.fitpulse.backend.common.ApiException;
import com.fitpulse.backend.common.OwnershipGuard;
import com.fitpulse.backend.exercise.MisicnaGrupa;
import com.fitpulse.backend.exercise.Vezba;
import com.fitpulse.backend.exercise.VezbaRepository;
import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.template.dto.TemplateRequest;
import com.fitpulse.backend.template.dto.TemplateResponse;
import com.fitpulse.backend.template.dto.TemplateVezbaRequest;
import com.fitpulse.backend.template.dto.TemplateVezbaResponse;
import com.fitpulse.backend.user.Korisnik;
import com.fitpulse.backend.user.KorisnikRepository;
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
    @Mock private VezbaRepository vezbaRepository;
    @Mock private KorisnikRepository korisnikRepository;

    private TemplateService templateService;
    private CustomUserDetails owner;
    private Template template;
    private Vezba bench;
    private Vezba squat;

    @BeforeEach
    void setUp() {
        templateService = new TemplateService(templateRepository, vezbaRepository, korisnikRepository, new OwnershipGuard());

        Korisnik korisnik = Korisnik.register("Pera", "Perić", "pera@example.com", "hash", null);
        korisnik.setId(1L);
        owner = new CustomUserDetails(korisnik);

        bench = vezba(10L, "Bench Press", null);
        squat = vezba(11L, "Čučanj", null);

        template = Template.create("Push", "stari opis", korisnik);
        template.setId(5L);
        template.addExercise(bench, 3, 8, new BigDecimal("60"));
    }

    private static Vezba vezba(Long id, String naziv, Korisnik createdBy) {
        Vezba vezba = Vezba.create(naziv, MisicnaGrupa.GRUDI, null, null, createdBy);
        vezba.setId(id);
        return vezba;
    }

    @Test
    void update_withoutExercises_shouldChangeOnlyNameAndDescription() {
        when(templateRepository.findWithExercisesById(5L)).thenReturn(Optional.of(template));

        TemplateResponse response = templateService.update(5L, new TemplateRequest("Push A", "novi opis", false, null), owner);

        assertThat(response.naziv()).isEqualTo("Push A");
        assertThat(response.opis()).isEqualTo("novi opis");
        assertThat(response.exercises()).extracting(TemplateVezbaResponse::vezbaNaziv).containsExactly("Bench Press");
        verify(templateRepository, never()).flush();
    }

    @Test
    void update_withExercises_shouldReplaceStructureInGivenOrder() {
        when(templateRepository.findWithExercisesById(5L)).thenReturn(Optional.of(template));
        when(vezbaRepository.findById(11L)).thenReturn(Optional.of(squat));
        when(vezbaRepository.findById(10L)).thenReturn(Optional.of(bench));

        List<TemplateVezbaRequest> newStructure = List.of(
                new TemplateVezbaRequest(11L, 5, 5, new BigDecimal("100")),
                new TemplateVezbaRequest(10L, 4, 10, null));

        TemplateResponse response = templateService.update(5L, new TemplateRequest("Push", null, false, newStructure), owner);

        assertThat(response.exercises())
                .extracting(TemplateVezbaResponse::vezbaNaziv, TemplateVezbaResponse::redniBroj)
                .containsExactly(tuple("Čučanj", 1), tuple("Bench Press", 2));
    }

    @Test
    void update_withExerciseOwnedByAnotherUser_shouldThrowBadRequest() {
        Korisnik drugi = Korisnik.register("Mika", "Mikić", "mika@example.com", "hash", null);
        drugi.setId(2L);
        Vezba tudja = vezba(20L, "Tuđa vežba", drugi);

        when(templateRepository.findWithExercisesById(5L)).thenReturn(Optional.of(template));
        when(vezbaRepository.findById(20L)).thenReturn(Optional.of(tudja));

        TemplateRequest request = new TemplateRequest("Push", null, false,
                List.of(new TemplateVezbaRequest(20L, 3, 8, null)));

        assertThatThrownBy(() -> templateService.update(5L, request, owner))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("nije dostupna");
    }
}
