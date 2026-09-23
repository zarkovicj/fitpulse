package com.fitpulse.backend.workout;

import com.fitpulse.backend.common.ApiException;
import com.fitpulse.backend.common.OwnershipGuard;
import com.fitpulse.backend.common.PageResponse;
import com.fitpulse.backend.exercise.Vezba;
import com.fitpulse.backend.exercise.VezbaRepository;
import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.template.Template;
import com.fitpulse.backend.template.TemplateRepository;
import com.fitpulse.backend.template.TemplateVezba;
import com.fitpulse.backend.user.KorisnikRepository;
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
public class TreningService {

    private static final int DEFAULT_SERIJE = 3;
    private static final int DEFAULT_PONAVLJANJA = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final TreningRepository treningRepository;
    private final TreningVezbaRepository treningVezbaRepository;
    private final TemplateRepository templateRepository;
    private final VezbaRepository vezbaRepository;
    private final KorisnikRepository korisnikRepository;
    private final OwnershipGuard ownershipGuard;
    private final ApplicationEventPublisher eventPublisher;

    public TreningService(TreningRepository treningRepository,
                          TreningVezbaRepository treningVezbaRepository,
                          TemplateRepository templateRepository,
                          VezbaRepository vezbaRepository,
                          KorisnikRepository korisnikRepository,
                          OwnershipGuard ownershipGuard,
                          ApplicationEventPublisher eventPublisher) {
        this.treningRepository = treningRepository;
        this.treningVezbaRepository = treningVezbaRepository;
        this.templateRepository = templateRepository;
        this.vezbaRepository = vezbaRepository;
        this.korisnikRepository = korisnikRepository;
        this.ownershipGuard = ownershipGuard;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TreningResponse start(StartTreningRequest request, CustomUserDetails principal) {
        Long userId = principal.getId();
        boolean hasExercises = request.exercises() != null && !request.exercises().isEmpty();

        if (request.templateId() != null && hasExercises) {
            throw ApiException.badRequest("Trening se pokreće ili iz template-a ili sa listom vežbi, ne oba");
        }
        if (treningRepository.existsByKorisnikIdAndStatus(userId, StatusTreninga.IN_PROGRESS)) {
            throw ApiException.conflict("Već imate trening u toku");
        }

        Template template = request.templateId() != null ? findUsableTemplate(request.templateId(), principal) : null;
        Trening trening = Trening.start(korisnikRepository.getReferenceById(userId), template);

        if (template != null) {
            for (TemplateVezba item : template.getExercises()) {
                TreningVezba exercise = trening.addExercise(item.getVezba());
                prefillSets(exercise, userId, item.getBrojSerija(), item.getBrojPonavljanja(), item.getKilaza());
            }
        } else if (hasExercises) {
            request.exercises().forEach(item -> addExerciseFromRequest(trening, item, userId));
        }

        return TreningResponse.from(treningRepository.save(trening));
    }

    @Transactional(readOnly = true)
    public Optional<TreningResponse> getActive(CustomUserDetails principal) {
        return treningRepository.findFirstByKorisnikIdAndStatus(principal.getId(), StatusTreninga.IN_PROGRESS)
                .map(TreningResponse::from);
    }

    @Transactional(readOnly = true)
    public TreningResponse getById(Long id, CustomUserDetails principal) {
        return TreningResponse.from(findOwnedOrThrow(id, principal));
    }

    @Transactional(readOnly = true)
    public PageResponse<TreningSummaryResponse> history(int page, int size, CustomUserDetails principal) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        return PageResponse.from(treningRepository.findHistory(principal.getId(), pageRequest)
                .map(TreningSummaryResponse::from));
    }

    @Transactional
    public TreningVezbaResponse addExercise(Long id, TreningVezbaRequest request, CustomUserDetails principal) {
        Trening trening = findInProgressOrThrow(id, principal);
        TreningVezba exercise = addExerciseFromRequest(trening, request, principal.getId());
        treningRepository.flush(); // da nova vežba i serije dobiju id pre mapiranja
        return TreningVezbaResponse.from(exercise);
    }

    @Transactional
    public void removeExercise(Long id, Long exerciseId, CustomUserDetails principal) {
        Trening trening = findInProgressOrThrow(id, principal);
        trening.removeExercise(findExerciseOrThrow(trening, exerciseId));
    }

    // nova serija kopira vrednosti poslednje, da korisnik ne kuca isto ponovo
    @Transactional
    public TreningSerijaResponse addSet(Long id, Long exerciseId, CustomUserDetails principal) {
        TreningVezba exercise = findExerciseOrThrow(findInProgressOrThrow(id, principal), exerciseId);
        List<TreningSerija> sets = exercise.getSets();

        TreningSerija set = sets.isEmpty()
                ? exercise.addSet(DEFAULT_PONAVLJANJA, null)
                : exercise.addSet(sets.getLast().getBrojPonavljanja(), sets.getLast().getKilaza());

        treningRepository.flush();
        return TreningSerijaResponse.from(set);
    }

    @Transactional
    public TreningSerijaResponse updateSet(Long id, Long exerciseId, Long setId, TreningSerijaRequest request,
                                           CustomUserDetails principal) {
        TreningVezba exercise = findExerciseOrThrow(findInProgressOrThrow(id, principal), exerciseId);
        TreningSerija set = findSetOrThrow(exercise, setId);

        exercise.updateSet(set, request.brojPonavljanja(), request.kilaza(), request.completed(), request.restAfter());
        return TreningSerijaResponse.from(set);
    }

    @Transactional
    public void removeSet(Long id, Long exerciseId, Long setId, CustomUserDetails principal) {
        TreningVezba exercise = findExerciseOrThrow(findInProgressOrThrow(id, principal), exerciseId);
        exercise.removeSet(findSetOrThrow(exercise, setId));
    }

    @Transactional
    public TreningResponse finish(Long id, FinishTreningRequest request, CustomUserDetails principal) {
        Trening trening = findInProgressOrThrow(id, principal);

        if (request != null && request.updateTemplate()) {
            Template template = trening.getTemplate();
            if (template == null) {
                throw ApiException.badRequest("Trening nije pokrenut iz template-a");
            }
            ownershipGuard.assertCanModify(template.getOwnerId(), principal);
            applyValuesToTemplate(trening, template);
        }

        trening.finish();
        eventPublisher.publishEvent(new TreningZavrsenEvent(trening.getId()));
        return TreningResponse.from(trening);
    }

    @Transactional
    public TreningResponse cancel(Long id, CustomUserDetails principal) {
        Trening trening = findInProgressOrThrow(id, principal);
        trening.cancel();
        return TreningResponse.from(trening);
    }

    // broj serija iz template-a, vrednosti iz poslednjeg završenog treninga sa tom vežbom (ako postoji)
    private void prefillSets(TreningVezba exercise, Long userId, int brojSerija, int ponavljanja, BigDecimal kilaza) {
        List<TreningSerija> previous = treningVezbaRepository
                .findLatestForUser(userId, exercise.getVezba().getId(), StatusTreninga.COMPLETED, Limit.of(1))
                .stream()
                .findFirst()
                .map(TreningVezba::getSets)
                .orElse(List.of());

        for (int i = 0; i < brojSerija; i++) {
            if (previous.isEmpty()) {
                exercise.addSet(ponavljanja, kilaza);
            } else {
                TreningSerija source = previous.get(Math.min(i, previous.size() - 1));
                exercise.addSet(source.getBrojPonavljanja(), source.getKilaza());
            }
        }
    }

    private TreningVezba addExerciseFromRequest(Trening trening, TreningVezbaRequest item, Long userId) {
        TreningVezba exercise = trening.addExercise(findUsableVezba(item.vezbaId(), userId));
        prefillSets(exercise, userId,
                item.brojSerija() != null ? item.brojSerija() : DEFAULT_SERIJE,
                item.brojPonavljanja() != null ? item.brojPonavljanja() : DEFAULT_PONAVLJANJA,
                item.kilaza());
        return exercise;
    }

    // za svaku vežbu iz template-a: broj serija iz treninga, ponavljanja i kilaža iz poslednje završene serije
    private void applyValuesToTemplate(Trening trening, Template template) {
        List<TreningVezba> remaining = new ArrayList<>(trening.getExercises());

        for (TemplateVezba item : template.getExercises()) {
            Optional<TreningVezba> match = remaining.stream()
                    .filter(exercise -> exercise.getVezba().getId().equals(item.getVezba().getId()))
                    .findFirst();
            if (match.isEmpty()) {
                continue;
            }
            remaining.remove(match.get());

            List<TreningSerija> sets = match.get().getSets();
            sets.stream().filter(TreningSerija::isCompleted).reduce((first, second) -> second)
                    .ifPresent(lastCompleted -> {
                        item.setBrojSerija(sets.size());
                        item.setBrojPonavljanja(lastCompleted.getBrojPonavljanja());
                        item.setKilaza(lastCompleted.getKilaza());
                    });
        }
    }

    private Template findUsableTemplate(Long templateId, CustomUserDetails principal) {
        return templateRepository.findWithExercisesById(templateId)
                .filter(template -> ownershipGuard.canView(template.getOwnerId(), principal))
                .orElseThrow(() -> ApiException.notFound("Template nije pronađen"));
    }

    private Vezba findUsableVezba(Long vezbaId, Long userId) {
        return vezbaRepository.findById(vezbaId)
                .filter(vezba -> vezba.getOwnerId() == null || vezba.getOwnerId().equals(userId))
                .orElseThrow(() -> ApiException.badRequest("Vežba " + vezbaId + " nije dostupna"));
    }

    // treninzi su strogo privatni, ni admin ne vidi tuđe
    private Trening findOwnedOrThrow(Long id, CustomUserDetails principal) {
        return treningRepository.findWithExercisesByIdAndKorisnikId(id, principal.getId())
                .orElseThrow(() -> ApiException.notFound("Trening nije pronađen"));
    }

    private Trening findInProgressOrThrow(Long id, CustomUserDetails principal) {
        Trening trening = findOwnedOrThrow(id, principal);
        if (!trening.isInProgress()) {
            throw ApiException.conflict("Trening nije u toku");
        }
        return trening;
    }

    private TreningVezba findExerciseOrThrow(Trening trening, Long exerciseId) {
        return trening.getExercises().stream()
                .filter(exercise -> exercise.getId().equals(exerciseId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Vežba nije pronađena u treningu"));
    }

    private TreningSerija findSetOrThrow(TreningVezba exercise, Long setId) {
        return exercise.getSets().stream()
                .filter(set -> set.getId().equals(setId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Serija nije pronađena"));
    }
}
