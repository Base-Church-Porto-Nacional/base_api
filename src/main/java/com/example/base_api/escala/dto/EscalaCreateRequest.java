package com.example.base_api.escala.dto;

import com.example.base_api.escala.entity.TipoEscala;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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

        @NotBlank(message = "Competência é obrigatória (YYYY-MM)")
        String competencia,

        @NotNull(message = "Tipo da escala é obrigatório")
        TipoEscala tipo,

        @NotNull(message = "igrejaId é obrigatório")
        UUID igrejaId,

        UUID ministerioId,

        UUID supervisorPessoaId,

        UUID pregadorPessoaId,

        LocalDate dataEvento,

        LocalTime horaInicio,

        LocalTime horaFim,

        @Size(max = 200, message = "Local deve ter no máximo 200 caracteres")
        String localEvento,

        UUID eventoId,

        @Valid
        List<EscalaServicoRequest> servicos,

        @Valid
        List<EscalaItemRequest> itens
) {
    public record EscalaServicoRequest(
            @NotNull(message = "servicoId é obrigatório")
            UUID servicoId,

            @NotNull(message = "quantidadeNecessaria é obrigatória")
            @Min(value = 1, message = "Quantidade deve ser no mínimo 1")
            Integer quantidadeNecessaria
    ) {
    }

    public record EscalaItemRequest(
            LocalDate data,
            UUID eventoId,
            UUID pessoaId,
            UUID servicoId,
            LocalTime horario
    ) {
    }
}
