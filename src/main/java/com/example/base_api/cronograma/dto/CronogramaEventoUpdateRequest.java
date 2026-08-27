package com.example.base_api.cronograma.dto;

import com.example.base_api.cronograma.entity.TipoEvento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CronogramaEventoUpdateRequest(
        @NotBlank(message = "Título é obrigatório")
        @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
        String titulo,

        @NotNull(message = "Tipo é obrigatório")
        TipoEvento tipo,

        String descricao,

        @NotNull(message = "Data do evento é obrigatória")
        LocalDate dataEvento,

        @NotNull(message = "Hora de início é obrigatória")
        LocalTime horaInicio,

        LocalTime horaFim,

        @Size(max = 200, message = "Local deve ter no máximo 200 caracteres")
        String localEvento,

        UUID supervisorPessoaId,

        @NotNull(message = "Campo ativo é obrigatório")
        Boolean ativo
) {
}
