package com.fitpulse.backend.progress;

import com.fitpulse.backend.common.ApiException;
import com.fitpulse.backend.common.OwnershipGuard;
import com.fitpulse.backend.exercise.VezbaRepository;
import com.fitpulse.backend.progress.dto.*;
import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.user.Korisnik;
import com.fitpulse.backend.user.KorisnikRepository;
import com.fitpulse.backend.workout.StatusTreninga;
import com.fitpulse.backend.workout.TreningRepository;
import com.fitpulse.backend.workout.TreningSerija;
import com.fitpulse.backend.workout.TreningSerijaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static com.fitpulse.backend.progress.RekordKalkulator.epley;
import static com.fitpulse.backend.progress.RekordKalkulator.hasWeight;

@Service
public class NapredakService {

    private final BodyGoalRepository bodyGoalRepository;
    private final BodyMasaLogRepository masaLogRepository;
    private final LicniRekordRepository rekordRepository;
    private final TreningRepository treningRepository;
    private final TreningSerijaRepository treningSerijaRepository;
    private final KorisnikRepository korisnikRepository;
    private final VezbaRepository vezbaRepository;
    private final OwnershipGuard ownershipGuard;

    public NapredakService(BodyGoalRepository bodyGoalRepository,
                           BodyMasaLogRepository masaLogRepository,
                           LicniRekordRepository rekordRepository,
                           TreningRepository treningRepository,
                           TreningSerijaRepository treningSerijaRepository,
                           KorisnikRepository korisnikRepository,
                           VezbaRepository vezbaRepository,
                           OwnershipGuard ownershipGuard) {
        this.bodyGoalRepository = bodyGoalRepository;
        this.masaLogRepository = masaLogRepository;
        this.rekordRepository = rekordRepository;
        this.treningRepository = treningRepository;
        this.treningSerijaRepository = treningSerijaRepository;
        this.korisnikRepository = korisnikRepository;
        this.vezbaRepository = vezbaRepository;
        this.ownershipGuard = ownershipGuard;
    }

    @Transactional(readOnly = true)
    public BodyGoalResponse getGoal(CustomUserDetails principal) {
        return bodyGoalRepository.findByKorisnikId(principal.getId())
                .map(goal -> new BodyGoalResponse(goal.getMasa(), goal.getProcenatMasti()))
                .orElse(new BodyGoalResponse(null, null));
    }

    @Transactional
    public BodyGoalResponse updateGoal(BodyGoalRequest request, CustomUserDetails principal) {
        BodyGoal goal = bodyGoalRepository.findByKorisnikId(principal.getId())
                .orElseGet(() -> BodyGoal.create(korisnikRepository.getReferenceById(principal.getId())));

        goal.setMasa(request.masa());
        goal.setProcenatMasti(request.procenatMasti());
        bodyGoalRepository.save(goal);

        return new BodyGoalResponse(goal.getMasa(), goal.getProcenatMasti());
    }

    // podrazumevano poslednjih godinu dana
    @Transactional(readOnly = true)
    public List<MasaLogResponse> weightHistory(LocalDate from, LocalDate to, CustomUserDetails principal) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusYears(1);
        if (start.isAfter(end)) {
            throw ApiException.badRequest("Početni datum mora biti pre krajnjeg");
        }
        return masaLogRepository.findByKorisnikIdAndDatumBetweenOrderByDatum(principal.getId(), start, end).stream()
                .map(MasaLogResponse::from)
                .toList();
    }

    // jedno merenje po danu: ponovni unos za isti dan menja postojeće
    @Transactional
    public MasaLogResponse logWeight(LocalDate datum, MasaRequest request, CustomUserDetails principal) {
        if (datum.isAfter(LocalDate.now())) {
            throw ApiException.badRequest("Merenje ne može biti u budućnosti");
        }

        Long userId = principal.getId();
        BodyMasaLog log = masaLogRepository.findByKorisnikIdAndDatum(userId, datum)
                .orElseGet(() -> BodyMasaLog.create(korisnikRepository.getReferenceById(userId), datum, request.masa()));
        log.setMasa(request.masa());
        masaLogRepository.save(log);

        syncCurrentWeight(userId);
        return MasaLogResponse.from(log);
    }

    @Transactional
    public void deleteWeight(LocalDate datum, CustomUserDetails principal) {
        Long userId = principal.getId();
        BodyMasaLog log = masaLogRepository.findByKorisnikIdAndDatum(userId, datum)
                .orElseThrow(() -> ApiException.notFound("Merenje za taj dan ne postoji"));
        masaLogRepository.delete(log);
        syncCurrentWeight(userId);
    }

    @Transactional(readOnly = true)
    public ProgressSummaryResponse summary(CustomUserDetails principal) {
        Long userId = principal.getId();
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        BigDecimal volume = treningSerijaRepository.sumVolumeSince(userId, StatusTreninga.COMPLETED, today.minusDays(30));
        Optional<BodyGoal> goal = bodyGoalRepository.findByKorisnikId(userId);

        return new ProgressSummaryResponse(
                treningRepository.countByKorisnikIdAndStatus(userId, StatusTreninga.COMPLETED),
                treningRepository.countByKorisnikIdAndStatusAndDatumGreaterThanEqual(userId, StatusTreninga.COMPLETED, monday),
                volume != null ? volume : BigDecimal.ZERO,
                rekordRepository.countByKorisnikId(userId),
                korisnikRepository.findById(userId).map(Korisnik::getMasa).orElse(null),
                goal.map(BodyGoal::getMasa).orElse(null),
                goal.map(BodyGoal::getProcenatMasti).orElse(null)
        );
    }

    @Transactional(readOnly = true)
    public List<VezbaNapredakResponse> exerciseHistory(Long vezbaId, CustomUserDetails principal) {
        vezbaRepository.findById(vezbaId)
                .filter(vezba -> ownershipGuard.canView(vezba.getOwnerId(), principal))
                .orElseThrow(() -> ApiException.notFound("Vežba nije pronađena"));

        Map<Long, List<TreningSerija>> setsByTrening = new LinkedHashMap<>();
        treningSerijaRepository.findCompletedSetsForExercise(principal.getId(), vezbaId, StatusTreninga.COMPLETED)
                .forEach(set -> setsByTrening
                        .computeIfAbsent(set.getTreningVezba().getTrening().getId(), id -> new ArrayList<>())
                        .add(set));

        return setsByTrening.values().stream().map(NapredakService::toPoint).toList();
    }

    private static VezbaNapredakResponse toPoint(List<TreningSerija> sets) {
        List<TreningSerija> weighted = sets.stream().filter(set -> hasWeight(set.getKilaza())).toList();

        return new VezbaNapredakResponse(
                sets.getFirst().getTreningVezba().getTrening().getId(),
                sets.getFirst().getTreningVezba().getTrening().getDatum(),
                weighted.stream().map(TreningSerija::getKilaza).max(Comparator.naturalOrder()).orElse(null),
                weighted.stream().map(set -> epley(set.getKilaza(), set.getBrojPonavljanja()))
                        .max(Comparator.naturalOrder()).orElse(null),
                sets.stream().mapToInt(TreningSerija::getBrojPonavljanja).max().orElse(0),
                weighted.stream().map(set -> set.getKilaza().multiply(BigDecimal.valueOf(set.getBrojPonavljanja())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    // korisnik.masa uvek prati poslednje merenje
    private void syncCurrentWeight(Long userId) {
        Korisnik korisnik = korisnikRepository.getReferenceById(userId);
        korisnik.setMasa(masaLogRepository.findFirstByKorisnikIdOrderByDatumDesc(userId)
                .map(BodyMasaLog::getMasa)
                .orElse(null));
    }
}
