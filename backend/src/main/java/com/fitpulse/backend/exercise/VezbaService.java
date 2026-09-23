package com.fitpulse.backend.exercise;

import com.fitpulse.backend.common.ApiException;
import com.fitpulse.backend.common.OwnershipGuard;
import com.fitpulse.backend.exercise.dto.VezbaRequest;
import com.fitpulse.backend.exercise.dto.VezbaResponse;
import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.user.KorisnikRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VezbaService {

    private final VezbaRepository vezbaRepository;
    private final KorisnikRepository korisnikRepository;
    private final OwnershipGuard ownershipGuard;

    public VezbaService(VezbaRepository vezbaRepository,
                         KorisnikRepository korisnikRepository,
                         OwnershipGuard ownershipGuard) {
        this.vezbaRepository = vezbaRepository;
        this.korisnikRepository = korisnikRepository;
        this.ownershipGuard = ownershipGuard;
    }

    @Transactional(readOnly = true)
    public List<VezbaResponse> search(MisicnaGrupa muscleGroup, String search, CustomUserDetails principal) {
        String normalizedSearch = search == null ? "" : search.trim(); // ne null: Postgres ne zna tip null parametra u LOWER()
        return vezbaRepository.search(ownershipGuard.viewerIdForQuery(principal), muscleGroup, normalizedSearch).stream()
                .map(VezbaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public VezbaResponse getById(Long id, CustomUserDetails principal) {
        return VezbaResponse.from(findVisibleOrThrow(id, principal));
    }

    @Transactional
    public VezbaResponse create(VezbaRequest request, CustomUserDetails principal) {
        Long ownerId = ownershipGuard.resolveOwnerIdForCreate(request.system(), principal);

        Vezba vezba = Vezba.create(
                request.naziv(),
                request.misicnaGrupa(),
                request.slika(),
                request.opis(),
                ownerId != null ? korisnikRepository.getReferenceById(ownerId) : null);

        return VezbaResponse.from(vezbaRepository.save(vezba));
    }

    @Transactional
    public VezbaResponse update(Long id, VezbaRequest request, CustomUserDetails principal) {
        Vezba vezba = findVisibleOrThrow(id, principal);
        ownershipGuard.assertCanModify(vezba.getOwnerId(), principal);

        vezba.setNaziv(request.naziv());
        vezba.setMisicnaGrupa(request.misicnaGrupa());
        vezba.setSlika(request.slika());
        vezba.setOpis(request.opis());

        return VezbaResponse.from(vezba);
    }

    @Transactional
    public void delete(Long id, CustomUserDetails principal) {
        Vezba vezba = findVisibleOrThrow(id, principal);
        ownershipGuard.assertCanModify(vezba.getOwnerId(), principal);

        vezbaRepository.delete(vezba);
        vezbaRepository.flush(); // da FK greška (vežba u upotrebi) pukne ovde, a ne pri commit-u
    }

    // tuđa privatna vežba se ponaša kao da ne postoji
    private Vezba findVisibleOrThrow(Long id, CustomUserDetails principal) {
        return vezbaRepository.findById(id)
                .filter(vezba -> ownershipGuard.canView(vezba.getOwnerId(), principal))
                .orElseThrow(() -> ApiException.notFound("Vežba nije pronađena"));
    }
}
