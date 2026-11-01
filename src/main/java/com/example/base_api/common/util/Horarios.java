package com.example.base_api.common.util;

import com.example.base_api.common.exception.BusinessException;

import java.time.LocalTime;

public final class Horarios {

    private Horarios() {
    }

    /** Exige que a hora fim seja posterior à hora início quando as duas forem informadas. */
    public static void validarFimAposInicio(LocalTime inicio, LocalTime fim) {
        if (inicio != null && fim != null && !fim.isAfter(inicio)) {
            throw new BusinessException("Hora fim deve ser posterior à hora início");
        }
    }
}
