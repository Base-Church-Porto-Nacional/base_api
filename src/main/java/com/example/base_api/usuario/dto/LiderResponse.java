package com.example.base_api.usuario.dto;

import java.util.Set;
import java.util.UUID;

public record LiderResponse(
        UUID usuarioId,
        String username,
        String email,
        UUID pessoaId,
        String pessoaNome,
        String telefone,
        Set<String> papeis
) {
}
