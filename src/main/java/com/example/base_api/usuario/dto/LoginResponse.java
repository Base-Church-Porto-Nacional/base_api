package com.example.base_api.usuario.dto;

import java.util.Set;
import java.util.UUID;

public record LoginResponse(
        String token,
        String tipo,
        UUID usuarioId,
        String username,
        Set<String> papeis
) {
}
