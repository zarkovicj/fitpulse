package com.fitpulse.backend.account;

import com.fitpulse.backend.common.ApiException;
import com.fitpulse.backend.image.ImageStorage;
import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.user.User;
import com.fitpulse.backend.user.UserRepository;
import com.fitpulse.backend.user.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ImageStorage imageStorage;
    private final PasswordEncoder passwordEncoder;

    public AccountDeletionService(UserRepository userRepository,
                                  AccountRepository accountRepository,
                                  ImageStorage imageStorage,
                                  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.imageStorage = imageStorage;
        this.passwordEncoder = passwordEncoder;
    }

    // redom: rekordi, treninzi, šabloni, pa nalog (baza kaskadno briše vežbe, ciljeve i merenja)
    @Transactional
    public void delete(User user) {
        Long userId = user.getId();
        List<String> imageKeys = accountRepository.findImageKeysOfUser(userId);
        accountRepository.deleteRecordsOfUser(userId);
        accountRepository.deleteWorkoutsOfUser(userId);
        accountRepository.deleteTemplatesOfUser(userId);
        userRepository.delete(user);
        userRepository.flush();
        imageKeys.forEach(imageStorage::delete);
        log.info("Obrisan nalog {}", user.getEmail());
    }

    @Transactional
    public void deleteOwn(String password, CustomUserDetails principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> ApiException.notFound("Korisnik nije pronađen"));

        if (user.getRole() == Role.ADMIN) {
            throw ApiException.forbidden("Administratorski nalog se ne briše iz aplikacije");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.badRequest("Pogrešna lozinka");
        }
        delete(user);
    }
}
