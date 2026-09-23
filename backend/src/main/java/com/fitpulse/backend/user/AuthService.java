package com.fitpulse.backend.user;

import com.fitpulse.backend.common.ApiException;
import com.fitpulse.backend.security.JwtTokenProvider;
import com.fitpulse.backend.user.dto.AuthResponse;
import com.fitpulse.backend.user.dto.LoginRequest;
import com.fitpulse.backend.user.dto.RegisterRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final KorisnikRepository korisnikRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(KorisnikRepository korisnikRepository,
                        PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        JwtTokenProvider jwtTokenProvider) {
        this.korisnikRepository = korisnikRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (korisnikRepository.existsByMail(request.mail())) {
            throw ApiException.conflict("Nalog sa ovim mail-om već postoji");
        }

        Korisnik korisnik = Korisnik.register(
                request.ime(), request.prezime(), request.mail(),
                passwordEncoder.encode(request.password()), request.datumRodjenja());

        korisnik = korisnikRepository.save(korisnik);

        String token = jwtTokenProvider.generateToken(korisnik.getId(), korisnik.getMail(), korisnik.getRole().name());
        return new AuthResponse(token, korisnik.getId(), korisnik.getMail(), korisnik.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.mail(), request.password())
        );

        Korisnik korisnik = korisnikRepository.findByMail(request.mail())
                .orElseThrow(() -> ApiException.unauthorized("Pogrešan mail ili lozinka"));

        String token = jwtTokenProvider.generateToken(korisnik.getId(), korisnik.getMail(), korisnik.getRole().name());
        return new AuthResponse(token, korisnik.getId(), korisnik.getMail(), korisnik.getRole().name());
    }
}
