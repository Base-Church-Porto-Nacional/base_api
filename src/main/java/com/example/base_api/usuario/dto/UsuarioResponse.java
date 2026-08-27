package com.example.base_api.usuario.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        UUID pessoaId,
        String pessoaNome,
        String username,
        String email,
        UUID igrejaId,
        String igrejaNome,
        Boolean ativo,
        Set<String> papeis,
        Instant createdAt,
        Instant updatedAt
) {
}
