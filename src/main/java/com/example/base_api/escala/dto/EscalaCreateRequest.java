package com.example.base_api.escala.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record EscalaCreateRequest(
        @NotBlank(message = "Título é obrigatório")
        @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
        String titulo,

        String descricao,

        @NotNull(message = "Data do evento é obrigatória")
        LocalDate dataEvento,

        @NotNull(message = "Hora de início é obrigatória")
        LocalTime horaInicio,

        LocalTime horaFim,

        @Size(max = 200, message = "Local deve ter no máximo 200 caracteres")
        String localEvento,

        @NotEmpty(message = "Informe ao menos um serviço necessário")
        @Valid
        List<EscalaServicoRequest> servicos
) {
    public record EscalaServicoRequest(
            @NotNull(message = "servicoId é obrigatório")
            UUID servicoId,

            @NotNull(message = "quantidadeNecessaria é obrigatória")
            @Min(value = 1, message = "Quantidade deve ser no mínimo 1")
            Integer quantidadeNecessaria
    ) {
    }
}
