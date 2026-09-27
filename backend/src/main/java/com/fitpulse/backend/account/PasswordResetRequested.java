package com.fitpulse.backend.account;

record PasswordResetRequested(String email, String firstName, String link) {

    @Override
    public String toString() {
        return "PasswordResetRequested[mail=" + email + "]";
    }
}
