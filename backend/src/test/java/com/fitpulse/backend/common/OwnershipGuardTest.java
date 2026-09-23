package com.fitpulse.backend.common;

import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.user.Korisnik;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnershipGuardTest {

    private final OwnershipGuard guard = new OwnershipGuard();

    private final CustomUserDetails user = principal(Korisnik.register("Pera", "Perić", "pera@example.com", "hash", null), 1L);
    private final CustomUserDetails admin = principal(Korisnik.createAdmin("Ana", "Anić", "ana@example.com", "hash"), 2L);

    private static CustomUserDetails principal(Korisnik korisnik, Long id) {
        korisnik.setId(id);
        return new CustomUserDetails(korisnik);
    }

    @Test
    void resolveOwnerIdForCreate_whenUserAsksForSystem_shouldStillReturnOwnId() {
        assertThat(guard.resolveOwnerIdForCreate(true, user)).isEqualTo(1L);
    }

    @Test
    void resolveOwnerIdForCreate_whenAdminAsksForSystem_shouldReturnNull() {
        assertThat(guard.resolveOwnerIdForCreate(true, admin)).isNull();
    }

    @Test
    void canView_whenResourceIsSystem_shouldAllowEveryone() {
        assertThat(guard.canView(null, user)).isTrue();
    }

    @Test
    void canView_whenResourceBelongsToAnotherUser_shouldDenyUserButAllowAdmin() {
        assertThat(guard.canView(99L, user)).isFalse();
        assertThat(guard.canView(99L, admin)).isTrue();
    }

    @Test
    void assertCanModify_whenUserIsOwner_shouldPass() {
        assertThatCode(() -> guard.assertCanModify(1L, user)).doesNotThrowAnyException();
    }

    @Test
    void assertCanModify_whenUserTouchesSystemResource_shouldThrowForbidden() {
        assertThatThrownBy(() -> guard.assertCanModify(null, user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus().value())
                .isEqualTo(403);
    }

    @Test
    void assertCanModify_whenAdminTouchesAnything_shouldPass() {
        assertThatCode(() -> guard.assertCanModify(null, admin)).doesNotThrowAnyException();
        assertThatCode(() -> guard.assertCanModify(99L, admin)).doesNotThrowAnyException();
    }
}
