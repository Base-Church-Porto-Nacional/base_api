package com.example.base_api.usuario.dto;

import java.time.Instant;
import java.util.UUID;

public record PapelResponse(
        UUID id,
        String nome,
        String descricao,
        UUID igrejaId,
        boolean sistema,
        Instant createdAt,
        Instant updatedAt
) {
}
