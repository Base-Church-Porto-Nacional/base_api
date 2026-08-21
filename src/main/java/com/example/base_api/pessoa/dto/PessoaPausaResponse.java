package com.example.base_api.pessoa.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PessoaPausaResponse(
        UUID id,
        UUID pessoaId,
        LocalDate dataInicio,
        LocalDate dataFim,
        String motivo,
        String observacoes,
        Boolean ativa,
        Instant createdAt,
        Instant updatedAt
) {
}
