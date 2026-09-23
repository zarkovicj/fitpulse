package com.fitpulse.backend.common;

import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.user.Role;
import org.springframework.stereotype.Component;

@Component
public class OwnershipGuard {

    // null = system-owned (samo ako je ADMIN kreirao i eksplicitno to traži)
    public Long resolveOwnerIdForCreate(boolean asSystem, CustomUserDetails principal) {
        if (asSystem && isAdmin(principal)) {
            return null;
        }
        return principal.getId();
    }

    public boolean canView(Long resourceOwnerId, CustomUserDetails principal) {
        return resourceOwnerId == null || isAdmin(principal) || resourceOwnerId.equals(principal.getId());
    }

    // null = bez filtera (admin vidi sve)
    public Long viewerIdForQuery(CustomUserDetails principal) {
        return isAdmin(principal) ? null : principal.getId();
    }

    public void assertCanModify(Long resourceOwnerId, CustomUserDetails principal) {
        boolean isOwner = resourceOwnerId != null && resourceOwnerId.equals(principal.getId());

        if (!isAdmin(principal) && !isOwner) {
            throw ApiException.forbidden("Nemate dozvolu da menjate ovaj resurs");
        }
    }

    private boolean isAdmin(CustomUserDetails principal) {
        return principal.getKorisnik().getRole() == Role.ADMIN;
    }
}
