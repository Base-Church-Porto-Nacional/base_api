package com.example.base_api.cronograma.dto;

import com.example.base_api.cronograma.entity.StatusEvento;
import com.example.base_api.cronograma.entity.TipoEvento;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CronogramaEventoResponse(
        UUID id,
        String titulo,
        String tema,
        TipoEvento tipo,
        String descricao,
        LocalDate dataEvento,
        LocalTime horaInicio,
        LocalTime horaFim,
        String localEvento,
        UUID igrejaId,
        String competencia,
        StatusEvento status,
        UUID supervisorPessoaId,
        String supervisorNome,
        Boolean ativo,
        List<MinisterioParticipante> ministerios,
        List<EscalaEventoResumo> escalas,
        Instant createdAt,
        Instant updatedAt
) {

    public record MinisterioParticipante(UUID id, String nome) {
    }

    public record EscalaEventoResumo(
            UUID id,
            UUID ministerioId,
            String ministerioNome,
            String titulo,
            Integer vagasTotais,
            Integer vagasPreenchidas
    ) {
    }
}