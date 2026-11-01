package com.example.base_api.common.entity;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;

import static org.assertj.core.api.Assertions.assertThat;

class DiaSemanaTest {

    @Test
    void converteParaDayOfWeekEDeVolta() {
        assertThat(DiaSemana.DOMINGO.toDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(DiaSemana.SEGUNDA.toDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(DiaSemana.SABADO.toDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
        for (DiaSemana dia : DiaSemana.values()) {
            assertThat(DiaSemana.de(dia.toDayOfWeek())).isEqualTo(dia);
        }
    }
}
