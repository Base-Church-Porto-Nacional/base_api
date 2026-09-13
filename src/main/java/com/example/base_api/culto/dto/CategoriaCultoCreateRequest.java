package com.example.base_api.culto.dto;

import com.example.base_api.culto.entity.DiaSemana;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.UUID;

public record CategoriaCultoCreateRequest(
        @NotBlank(message = "Título é obrigatório")
        @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
        String titulo,

        String descricao,

        @NotNull(message = "Dia da semana é obrigatório")
        DiaSemana diaSemana,

        LocalTime horaInicio,

        @Positive(message = "Duração deve ser maior que zero")
        Integer duracaoMinutos,

        UUID pregadorPadraoId,

        UUID igrejaId
) {
}