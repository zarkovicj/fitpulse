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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(User.normalizeEmail(request.email()))) {
            throw ApiException.conflict("Nalog sa ovim mail-om već postoji");
        }

        User user = User.register(
                request.firstName(), request.lastName(), request.email(),
                passwordEncoder.encode(request.password()), request.birthDate());

        user = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(User.normalizeEmail(request.email()))
                .orElseThrow(() -> ApiException.unauthorized("Pogrešan mail ili lozinka"));

        String token = jwtTokenProvider.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole().name());
    }
}
