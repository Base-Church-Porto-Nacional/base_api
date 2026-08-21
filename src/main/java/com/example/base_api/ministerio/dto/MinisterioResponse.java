package com.example.base_api.ministerio.dto;

import java.time.Instant;
import java.util.UUID;

public record MinisterioResponse(
        UUID id,
        String nome,
        String descricao,
        Boolean ativo,
        Instant createdAt,
        Instant updatedAt
) {
}
