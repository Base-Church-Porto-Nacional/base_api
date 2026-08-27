package com.example.base_api.escala.dto;

import com.example.base_api.escala.entity.TipoEscala;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record EscalaUpdateRequest(
        @NotBlank(message = "Título é obrigatório")
        @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
        String titulo,

        String descricao,

        String competencia,

        TipoEscala tipo,

        UUID ministerioId,

        UUID supervisorPessoaId,

        UUID pregadorPessoaId,

        LocalDate dataEvento,

        LocalTime horaInicio,

        LocalTime horaFim,

        @Size(max = 200, message = "Local deve ter no máximo 200 caracteres")
        String localEvento,

        UUID eventoId,

        @NotNull(message = "Campo ativo é obrigatório")
        Boolean ativo
) {
}
