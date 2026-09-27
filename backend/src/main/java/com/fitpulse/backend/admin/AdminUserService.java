package com.fitpulse.backend.admin;

import com.fitpulse.backend.account.AccountDeletionService;
import com.fitpulse.backend.admin.dto.AdminUserResponse;
import com.fitpulse.backend.common.ApiException;
import com.fitpulse.backend.common.LikeSearch;
import com.fitpulse.backend.common.PageResponse;
import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.user.User;
import com.fitpulse.backend.user.UserRepository;
import com.fitpulse.backend.user.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminUserRepository adminUserRepository;
    private final UserRepository userRepository;
    private final AccountDeletionService accountDeletionService;

    public AdminUserService(AdminUserRepository adminUserRepository,
                            UserRepository userRepository,
                            AccountDeletionService accountDeletionService) {
        this.adminUserRepository = adminUserRepository;
        this.userRepository = userRepository;
        this.accountDeletionService = accountDeletionService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> search(String search, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        return PageResponse.from(adminUserRepository.search(LikeSearch.escape(search), pageRequest));
    }

    @Transactional
    public AdminUserResponse setActive(Long id, boolean active, CustomUserDetails principal) {
        User user = findManageableOrThrow(id, principal);
        user.setActive(active);
        log.info("Admin {} je {} nalog {}", principal.getUsername(), active ? "odblokirao" : "blokirao", user.getEmail());
        return toResponse(user);
    }

    @Transactional
    public void delete(Long id, CustomUserDetails principal) {
        User user = findManageableOrThrow(id, principal);
        accountDeletionService.delete(user);
        log.info("Admin {} je obrisao nalog {}", principal.getUsername(), user.getEmail());
    }

    // admin ne sme sebe da zakljuca
    // admini se prave samo kroz konfiguraciju
    private User findManageableOrThrow(Long id, CustomUserDetails principal) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Korisnik nije pronađen"));
        if (user.getId().equals(principal.getId())) {
            throw ApiException.badRequest("Ne možeš da blokiraš ni obrišeš svoj nalog");
        }
        if (user.getRole() == Role.ADMIN) {
            throw ApiException.forbidden("Nalog administratora ne može da se blokira ni obriše");
        }
        return user;
    }

    private AdminUserResponse toResponse(User user) {
        return adminUserRepository.findSummary(user.getId()).orElseThrow();
    }
}
