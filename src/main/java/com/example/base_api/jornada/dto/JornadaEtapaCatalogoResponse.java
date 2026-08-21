package com.example.base_api.jornada.dto;

import java.time.Instant;
import java.util.UUID;

public record JornadaEtapaCatalogoResponse(
        UUID id,
        String codigo,
        String nome,
        String descricao,
        Integer ordem,
        Boolean ativo,
        Instant createdAt,
        Instant updatedAt
) {
}
