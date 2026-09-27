package com.fitpulse.backend.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private AdminInitializer initializer(AdminProperties properties) {
        return new AdminInitializer(properties, userRepository, passwordEncoder);
    }

    @Test
    void run_withConfiguredMail_shouldCreateAdminWithEncodedPassword() {
        when(userRepository.existsByEmail("admin@fitpulse.local")).thenReturn(false);
        when(passwordEncoder.encode("tajna1234")).thenReturn("hash");

        initializer(new AdminProperties("admin@fitpulse.local", "tajna1234", null, null)).run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hash");
        assertThat(saved.getValue().getFirstName()).isEqualTo("Admin");
    }

    @Test
    void run_whenAdminAlreadyExists_shouldNotCreateAnother() {
        when(userRepository.existsByEmail("admin@fitpulse.local")).thenReturn(true);

        initializer(new AdminProperties("admin@fitpulse.local", "tajna1234", null, null)).run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void run_withoutMail_shouldDoNothing() {
        initializer(new AdminProperties("", null, null, null)).run(null);

        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void properties_withShortPassword_shouldFailOnStartup() {
        assertThatThrownBy(() -> new AdminProperties("admin@fitpulse.local", "kratka", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void properties_toString_shouldNotRevealPassword() {
        assertThat(new AdminProperties("admin@fitpulse.local", "tajna1234", null, null).toString())
                .doesNotContain("tajna1234");
    }
}
