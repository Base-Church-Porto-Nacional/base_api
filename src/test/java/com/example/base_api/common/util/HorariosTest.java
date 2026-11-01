package com.example.base_api.common.util;

import com.example.base_api.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HorariosTest {

    @Test
    void rejeitaFimIgualOuAntesDoInicio() {
        assertThatThrownBy(() -> Horarios.validarFimAposInicio(LocalTime.of(19, 0), LocalTime.of(19, 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Hora fim deve ser posterior à hora início");
        assertThatThrownBy(() -> Horarios.validarFimAposInicio(LocalTime.of(19, 0), LocalTime.of(18, 0)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void aceitaFimDepoisDoInicioOuHorarioIncompleto() {
        assertThatCode(() -> Horarios.validarFimAposInicio(LocalTime.of(19, 0), LocalTime.of(21, 0)))
                .doesNotThrowAnyException();
        assertThatCode(() -> Horarios.validarFimAposInicio(LocalTime.of(19, 0), null)).doesNotThrowAnyException();
        assertThatCode(() -> Horarios.validarFimAposInicio(null, LocalTime.of(21, 0))).doesNotThrowAnyException();
    }
}
