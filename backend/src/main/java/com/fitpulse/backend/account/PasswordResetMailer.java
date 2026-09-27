package com.fitpulse.backend.account;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
class PasswordResetMailer {

    private final JavaMailSender mailSender;
    private final AccountMailProperties mailProperties;

    PasswordResetMailer(JavaMailSender mailSender, AccountMailProperties mailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onResetRequested(PasswordResetRequested event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(event.email());
        message.setSubject("FitPulse: promena lozinke");
        message.setText("""
                Zdravo %s,

                Primili smo zahtev za promenu lozinke za tvoj FitPulse nalog.
                Novu lozinku možeš da postaviš preko ovog linka, koji važi %d minuta i može da se iskoristi jednom:

                %s

                Ako nisi zatražio promenu lozinke, zanemari ovaj mail. Tvoja lozinka ostaje nepromenjena.
                """.formatted(event.firstName(), PasswordResetService.VALIDITY.toMinutes(), event.link()));
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Slanje maila za promenu lozinke na {} nije uspelo", event.email(), e);
        }
    }
}
