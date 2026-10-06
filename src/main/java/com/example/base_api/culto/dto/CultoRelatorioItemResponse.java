package com.example.base_api.culto.dto;

import com.example.base_api.culto.entity.StatusCulto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Linha do relatório consolidado: um culto do período e a sua contagem
 * ({@code relatorio} nulo quando ainda não foi lançada).
 */
public record CultoRelatorioItemResponse(
        UUID cultoId,
        UUID igrejaId,
        UUID categoriaCultoId,
        String categoriaCultoTitulo,
        String titulo,
        LocalDate data,
        LocalTime horaInicio,
        StatusCulto status,
        Boolean temRelatorio,
        CultoRelatorioResponse relatorio
) {
}
