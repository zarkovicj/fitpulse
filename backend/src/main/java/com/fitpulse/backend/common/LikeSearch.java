package com.fitpulse.backend.common;

public final class LikeSearch {

    private LikeSearch() {
    }

    public static String escape(String search) {
        if (search == null) {
            return "";
        }
        return search.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
