package com.fitpulse.backend.progress;

import com.fitpulse.backend.exercise.Vezba;
import com.fitpulse.backend.progress.dto.LicniRekordResponse;
import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.workout.Trening;
import com.fitpulse.backend.workout.TreningRepository;
import com.fitpulse.backend.workout.TreningSerija;
import com.fitpulse.backend.workout.TreningVezba;
import com.fitpulse.backend.workout.TreningZavrsenEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.fitpulse.backend.progress.RekordKalkulator.epley;
import static com.fitpulse.backend.progress.RekordKalkulator.hasWeight;
import static com.fitpulse.backend.progress.RekordKalkulator.orZero;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;

@Service
public class LicniRekordService {

    private final LicniRekordRepository rekordRepository;
    private final TreningRepository treningRepository;

    public LicniRekordService(LicniRekordRepository rekordRepository, TreningRepository treningRepository) {
        this.rekordRepository = rekordRepository;
        this.treningRepository = treningRepository;
    }

    // sinhrono, u istoj transakciji kao finish: ako ovo pukne, ni trening se ne završava
    @EventListener
    @Transactional
    public void onTreningZavrsen(TreningZavrsenEvent event) {
        treningRepository.findById(event.treningId()).ifPresent(this::evaluate);
    }

    @Transactional
    public List<LicniRekord> evaluate(Trening trening) {
        Map<Long, List<TreningSerija>> completedSetsByVezba = new LinkedHashMap<>();
        Map<Long, Vezba> vezbe = new HashMap<>();

        for (TreningVezba exercise : trening.getExercises()) {
            Long vezbaId = exercise.getVezba().getId();
            vezbe.put(vezbaId, exercise.getVezba());
            exercise.getSets().stream()
                    .filter(TreningSerija::isCompleted)
                    .forEach(set -> completedSetsByVezba.computeIfAbsent(vezbaId, id -> new ArrayList<>()).add(set));
        }
        if (completedSetsByVezba.isEmpty()) {
            return List.of();
        }

        Map<Kljuc, LicniRekord> existing = rekordRepository
                .findByKorisnikIdAndVezbaIdIn(trening.getKorisnik().getId(), completedSetsByVezba.keySet())
                .stream()
                .collect(Collectors.toMap(r -> new Kljuc(r.getVezba().getId(), r.getTip()), Function.identity()));

        List<LicniRekord> changed = new ArrayList<>();
        completedSetsByVezba.forEach((vezbaId, sets) -> {
            Vezba vezba = vezbe.get(vezbaId);

            sets.stream()
                    .filter(set -> hasWeight(set.getKilaza()))
                    .max(comparing(TreningSerija::getKilaza).thenComparingInt(TreningSerija::getBrojPonavljanja))
                    .ifPresent(best -> apply(TipRekorda.MAX_WEIGHT, best, vezba, trening, existing, changed));

            sets.stream()
                    .filter(set -> hasWeight(set.getKilaza()) && set.getBrojPonavljanja() >= 1)
                    .max(comparing(set -> epley(set.getKilaza(), set.getBrojPonavljanja())))
                    .ifPresent(best -> apply(TipRekorda.ESTIMATED_1RM, best, vezba, trening, existing, changed));

            sets.stream()
                    .filter(set -> set.getBrojPonavljanja() >= 1)
                    .max(comparingInt(TreningSerija::getBrojPonavljanja).thenComparing(set -> orZero(set.getKilaza())))
                    .ifPresent(best -> apply(TipRekorda.MAX_REPS, best, vezba, trening, existing, changed));
        });

        return rekordRepository.saveAll(changed);
    }

    @Transactional(readOnly = true)
    public List<LicniRekordResponse> search(Long vezbaId, Long treningId, CustomUserDetails principal) {
        return rekordRepository.search(principal.getId(), vezbaId, treningId).stream()
                .map(LicniRekordResponse::from)
                .toList();
    }

    private void apply(TipRekorda tip, TreningSerija set, Vezba vezba, Trening trening,
                       Map<Kljuc, LicniRekord> existing, List<LicniRekord> changed) {
        BigDecimal e1rm = hasWeight(set.getKilaza()) ? epley(set.getKilaza(), set.getBrojPonavljanja()) : null;
        LicniRekord current = existing.get(new Kljuc(vezba.getId(), tip));
        if (!isBetter(tip, set, e1rm, current)) {
            return;
        }

        LicniRekord rekord = current != null ? current : LicniRekord.create(trening.getKorisnik(), vezba, tip);
        rekord.record(set.getBrojPonavljanja(), set.getKilaza(), e1rm, trening.getId(), trening.getFinishedAt());
        changed.add(rekord);
    }

    // kod izjednačenja odlučuje drugi kriterijum (npr. ista kilaža, više ponavljanja)
    private static boolean isBetter(TipRekorda tip, TreningSerija set, BigDecimal e1rm, LicniRekord current) {
        if (current == null) {
            return true;
        }
        return switch (tip) {
            case MAX_WEIGHT -> {
                int cmp = set.getKilaza().compareTo(current.getKilaza());
                yield cmp > 0 || (cmp == 0 && set.getBrojPonavljanja() > current.getPonavljanja());
            }
            case ESTIMATED_1RM -> e1rm.compareTo(current.getEstimated1rm()) > 0;
            case MAX_REPS -> set.getBrojPonavljanja() > current.getPonavljanja()
                    || (set.getBrojPonavljanja() == current.getPonavljanja()
                        && orZero(set.getKilaza()).compareTo(orZero(current.getKilaza())) > 0);
        };
    }

    private record Kljuc(Long vezbaId, TipRekorda tip) {
    }
}
