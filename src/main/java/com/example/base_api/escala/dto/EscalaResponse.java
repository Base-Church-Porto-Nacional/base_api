package com.example.base_api.escala.dto;

import com.example.base_api.escala.entity.StatusEscalaVoluntario;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record EscalaResponse(
        UUID id,
        String titulo,
        String descricao,
        LocalDate dataEvento,
        LocalTime horaInicio,
        LocalTime horaFim,
        String localEvento,
        Boolean ativo,
        List<EscalaServicoResponse> servicos,
        List<EscalaVoluntarioResponse> voluntarios,
        Instant createdAt,
        Instant updatedAt
) {
    public record EscalaServicoResponse(
            UUID id,
            UUID servicoId,
            String servicoNome,
            Integer quantidadeNecessaria
    ) {
    }

    public record EscalaVoluntarioResponse(
            UUID id,
            UUID pessoaId,
            String pessoaNome,
            UUID servicoId,
            String servicoNome,
            StatusEscalaVoluntario status,
            String observacoes,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
