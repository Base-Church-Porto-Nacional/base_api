package com.example.base_api.common.entity;

import java.time.DayOfWeek;

/** Dia da semana usado nas categorias de culto, disponibilidades e pequenos grupos. */
public enum DiaSemana {
    DOMINGO(DayOfWeek.SUNDAY),
    SEGUNDA(DayOfWeek.MONDAY),
    TERCA(DayOfWeek.TUESDAY),
    QUARTA(DayOfWeek.WEDNESDAY),
    QUINTA(DayOfWeek.THURSDAY),
    SEXTA(DayOfWeek.FRIDAY),
    SABADO(DayOfWeek.SATURDAY);

    private final DayOfWeek dayOfWeek;

    DiaSemana(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public DayOfWeek toDayOfWeek() {
        return dayOfWeek;
    }

    public static DiaSemana de(DayOfWeek dayOfWeek) {
        for (DiaSemana dia : values()) {
            if (dia.dayOfWeek == dayOfWeek) {
                return dia;
            }
        }
        throw new IllegalArgumentException("Dia da semana inválido: " + dayOfWeek);
    }
}
