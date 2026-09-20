package com.fitpulse.backend.user;

import com.fitpulse.backend.common.ApiException;
import com.fitpulse.backend.user.dto.UpdateUserRequest;
import com.fitpulse.backend.user.dto.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final KorisnikRepository korisnikRepository;

    public UserService(KorisnikRepository korisnikRepository) {
        this.korisnikRepository = korisnikRepository;
    }

    public UserResponse getMe(Long userId) {
        return UserResponse.from(findOrThrow(userId));
    }

    @Transactional
    public UserResponse updateMe(Long userId, UpdateUserRequest request) {
        Korisnik korisnik = findOrThrow(userId);

        korisnik.setIme(request.ime());
        korisnik.setPrezime(request.prezime());
        korisnik.setDatumRodjenja(request.datumRodjenja());
        korisnik.setMasa(request.masa());
        korisnik.setVisina(request.visina());
        korisnik.setCiljnaMasa(request.ciljnaMasa());

        return UserResponse.from(korisnik);
    }

    private Korisnik findOrThrow(Long userId) {
        return korisnikRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Korisnik nije pronađen"));
    }
}
