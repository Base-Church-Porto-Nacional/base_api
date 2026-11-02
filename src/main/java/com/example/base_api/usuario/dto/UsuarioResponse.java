package com.example.base_api.usuario.dto;

import com.example.base_api.usuario.entity.StatusUsuario;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        UUID pessoaId,
        String pessoaNome,
        String email,
        String telefone,
        UUID igrejaId,
        String igrejaNome,
        StatusUsuario status,
        Set<String> papeis,
        Instant createdAt,
        Instant updatedAt
) {
}
