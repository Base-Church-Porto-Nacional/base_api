package com.example.base_api.common.util;

public final class Textos {

    private Textos() {
    }

    /** Texto aparado, ou {@code null} quando vazio ou só com espaços. */
    public static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
