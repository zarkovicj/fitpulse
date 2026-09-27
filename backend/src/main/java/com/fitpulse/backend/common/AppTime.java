package com.fitpulse.backend.common;

import java.time.LocalDate;
import java.time.ZoneId;

public final class AppTime {

    public static final ZoneId ZONE = ZoneId.of("Europe/Belgrade");

    private AppTime() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
