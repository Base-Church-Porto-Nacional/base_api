package com.example.base_api.escala.dto;

import com.example.base_api.escala.entity.StatusEscala;
import com.example.base_api.escala.entity.StatusEscalaVoluntario;
import com.example.base_api.escala.entity.TipoEscala;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record EscalaResponse(
        UUID id,
        String titulo,
        String descricao,
        String competencia,
        TipoEscala tipo,
        UUID igrejaId,
        UUID ministerioId,
        String ministerioNome,
        UUID supervisorPessoaId,
        String supervisorNome,
        UUID pregadorPessoaId,
        String pregadorNome,
        LocalDate dataEvento,
        LocalTime horaInicio,
        LocalTime horaFim,
        String localEvento,
        UUID eventoId,
        Boolean ativo,
        StatusEscala status,
        Instant dataLancamento,
        Instant dataFinalizacao,
        List<EscalaItemResponse> itens,
        List<EscalaServicoResponse> servicos,
        List<EscalaVoluntarioResponse> voluntarios,
        Instant createdAt,
        Instant updatedAt
) {
    public record EscalaItemResponse(
            UUID id,
            LocalDate data,
            UUID eventoId,
            UUID pessoaId,
            String pessoaNome,
            UUID servicoId,
            String servicoNome,
            LocalTime horario,
            StatusEscalaVoluntario status,
            Boolean confirmadoPeloVoluntario
    ) {
    }

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
            Boolean confirmadoPeloVoluntario,
            String observacoes,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
