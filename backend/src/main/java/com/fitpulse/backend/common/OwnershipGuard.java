package com.fitpulse.backend.common;

import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.user.Role;
import org.springframework.stereotype.Component;

/**
 * Pravila vlasnistva:
 * korisnik radi sa svojim i sistemskim resursima,
 * a admin upravlja samo sistemskim i ne vidi privatne resurse korisnika.
 */
@Component
public class OwnershipGuard {

    // null - sistemski resurs
    public Long resolveOwnerIdForCreate(CustomUserDetails principal) {
        return isAdmin(principal) ? null : principal.getId();
    }

    public boolean canView(Long resourceOwnerId, CustomUserDetails principal) {
        return resourceOwnerId == null || resourceOwnerId.equals(principal.getId());
    }

    public Long viewerIdForQuery(CustomUserDetails principal) {
        return principal.getId();
    }

    public void assertCanModify(Long resourceOwnerId, CustomUserDetails principal) {
        boolean allowed = isAdmin(principal)
                ? resourceOwnerId == null
                : resourceOwnerId != null && resourceOwnerId.equals(principal.getId());

        if (!allowed) {
            throw ApiException.forbidden("Nemate dozvolu da menjate ovaj resurs");
        }
    }

    private boolean isAdmin(CustomUserDetails principal) {
        return principal.getUser().getRole() == Role.ADMIN;
    }
}
