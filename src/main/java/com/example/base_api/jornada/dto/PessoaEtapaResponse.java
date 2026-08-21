package com.example.base_api.jornada.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PessoaEtapaResponse(
        UUID id,
        UUID pessoaId,
        UUID etapaId,
        String etapaCodigo,
        String etapaNome,
        Boolean concluida,
        LocalDate dataConclusao,
        String observacoes,
        Instant createdAt,
        Instant updatedAt
) {
}
