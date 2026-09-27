package com.fitpulse.backend.account;

import com.fitpulse.backend.TestcontainersConfig;
import com.fitpulse.backend.account.dto.AccountRequests;
import com.fitpulse.backend.user.AuthService;
import com.fitpulse.backend.user.User;
import com.fitpulse.backend.user.UserRepository;
import com.fitpulse.backend.user.dto.LoginRequest;
import com.fitpulse.backend.user.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfig.class)
class AccountIntegrationTest {

    private static final Pattern TOKEN = Pattern.compile("reset-password\\?token=([A-Za-z0-9_-]+)");

    @Autowired private RestTestClient restTestClient;
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private JavaMailSender mailSender;

    @BeforeEach
    void resetMailMock() {
        clearInvocations(mailSender);
    }

    private String register(String email) {
        return authService.register(new RegisterRequest("Ana", "Test", email, "stara123", null)).token();
    }

    private RestTestClient.ResponseSpec forgot(String email) {
        return restTestClient.post().uri("/api/auth/forgot-password")
                .body(new AccountRequests.ForgotPassword(email))
                .exchange();
    }

    private RestTestClient.ResponseSpec reset(String token, String password) {
        return restTestClient.post().uri("/api/auth/reset-password")
                .body(new AccountRequests.ResetPassword(token, password))
                .exchange();
    }

    private RestTestClient.ResponseSpec login(String email, String password) {
        return restTestClient.post().uri("/api/auth/login").body(new LoginRequest(email, password)).exchange();
    }

    private String sentToken(String expectedRecipient) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly(expectedRecipient);
        Matcher matcher = TOKEN.matcher(message.getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    @Test
    void forgotPassword_forUnknownMail_shouldLookTheSameButSendNothing() {
        forgot("nepostoji@example.com").expectStatus().isNoContent();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void resetPassword_shouldChangePasswordAndInvalidateOldTokensAndLink() {
        String email = "reset@example.com";
        String oldJwt = register(email);

        forgot(email).expectStatus().isNoContent();
        String token = sentToken(email);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM password_reset_token WHERE token_hash = ?",
                Integer.class, token)).isZero();

        reset(token, "kr").expectStatus().isBadRequest();

        // ista lozinka kao stara se odbija, a link i dalje vazi
        reset(token, "stara123").expectStatus().isBadRequest();
        reset(token, "nova1234").expectStatus().isNoContent();

        login(email, "stara123").expectStatus().isUnauthorized();
        login(email, "nova1234").expectStatus().isOk();

        // token od pre promene lozinke vise ne vazi
        restTestClient.get().uri("/api/user/me").header("Authorization", "Bearer " + oldJwt)
                .exchange().expectStatus().isUnauthorized();

        // link je jednokratan
        reset(token, "treca1234").expectStatus().isBadRequest();
    }

    @Test
    void resetLink_shouldExpireAndOnlyTheLatestShouldWork() {
        String email = "reset-expiry@example.com";
        register(email);

        forgot(email);
        String first = sentToken(email);
        clearInvocations(mailSender);
        forgot(email);
        String second = sentToken(email);

        reset(first, "nova1234").expectStatus().isBadRequest();

        jdbcTemplate.update("UPDATE password_reset_token SET expires_at = NOW() - INTERVAL '1 minute'");
        reset(second, "nova1234").expectStatus().isBadRequest();
    }

    @Test
    void deleteOwnAccount_shouldRequirePasswordAndRemoveAccount() {
        String email = "self-delete@example.com";
        String jwt = register(email);

        restTestClient.method(org.springframework.http.HttpMethod.DELETE).uri("/api/user/me")
                .header("Authorization", "Bearer " + jwt)
                .body(new AccountRequests.DeleteAccount("pogresna"))
                .exchange()
                .expectStatus().isBadRequest();
        assertThat(userRepository.findByEmail(email)).isPresent();

        restTestClient.method(org.springframework.http.HttpMethod.DELETE).uri("/api/user/me")
                .header("Authorization", "Bearer " + jwt)
                .body(new AccountRequests.DeleteAccount("stara123"))
                .exchange()
                .expectStatus().isNoContent();
        assertThat(userRepository.findByEmail(email)).isEmpty();
        login(email, "stara123").expectStatus().isUnauthorized();
    }

    @Test
    void deleteOwnAccount_asAdmin_shouldBeForbidden() {
        userRepository.save(User.createAdmin("Admin", "Test", "self-admin@example.com", passwordEncoder.encode("adminlozinka")));
        String jwt = authService.login(new LoginRequest("self-admin@example.com", "adminlozinka")).token();

        restTestClient.method(org.springframework.http.HttpMethod.DELETE).uri("/api/user/me")
                .header("Authorization", "Bearer " + jwt)
                .body(new AccountRequests.DeleteAccount("adminlozinka"))
                .exchange()
                .expectStatus().isForbidden();
    }
}
