package com.fitpulse.backend.common;

import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.user.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnershipGuardTest {

    private final OwnershipGuard guard = new OwnershipGuard();

    private final CustomUserDetails user = principal(User.register("Pera", "Perić", "pera@example.com", "hash", null), 1L);
    private final CustomUserDetails admin = principal(User.createAdmin("Ana", "Anić", "ana@example.com", "hash"), 2L);

    private static CustomUserDetails principal(User user, Long id) {
        user.setId(id);
        return new CustomUserDetails(user);
    }

    @Test
    void resolveOwnerIdForCreate_forUser_shouldReturnOwnId() {
        assertThat(guard.resolveOwnerIdForCreate(user)).isEqualTo(1L);
    }

    @Test
    void resolveOwnerIdForCreate_forAdmin_shouldCreateSystemResource() {
        assertThat(guard.resolveOwnerIdForCreate(admin)).isNull();
    }

    @Test
    void canView_whenResourceIsSystem_shouldAllowEveryone() {
        assertThat(guard.canView(null, user)).isTrue();
        assertThat(guard.canView(null, admin)).isTrue();
    }

    @Test
    void canView_whenResourceBelongsToAnotherUser_shouldDenyUserAndAdmin() {
        assertThat(guard.canView(99L, user)).isFalse();
        assertThat(guard.canView(99L, admin)).isFalse();
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
    void assertCanModify_forAdmin_shouldAllowOnlySystemResources() {
        assertThatCode(() -> guard.assertCanModify(null, admin)).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.assertCanModify(99L, admin))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus().value())
                .isEqualTo(403);
    }
}