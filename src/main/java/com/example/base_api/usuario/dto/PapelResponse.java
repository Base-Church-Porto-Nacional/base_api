package com.example.base_api.usuario.dto;

import java.time.Instant;
import java.util.UUID;

public record PapelResponse(
        UUID id,
        String nome,
        String descricao,
        Instant createdAt,
        Instant updatedAt
) {
}
