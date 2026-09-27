package com.fitpulse.backend.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

// ako je prazan mail, ne pravi se admin nalog
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String email, String password, String firstName, String lastName) {

    public AdminProperties {
        if (email != null && !email.isBlank() && (password == null || password.length() < 8)) {
            throw new IllegalArgumentException("admin_password mora imati bar 8 karaktera kad je admin_mail zadat");
        }
    }

    public boolean isConfigured() {
        return email != null && !email.isBlank();
    }

    // lozinka ne sme da zavrsi u logu
    @Override
    public String toString() {
        return "AdminProperties[mail=" + email + ", password=***]";
    }
}
