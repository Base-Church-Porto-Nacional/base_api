package com.example.base_api.servico.dto;

import java.time.Instant;
import java.util.UUID;

public record ServicoResponse(
        UUID id,
        UUID ministerioId,
        String nome,
        String descricao,
        Boolean ativo,
        Instant createdAt,
        Instant updatedAt
) {
}
