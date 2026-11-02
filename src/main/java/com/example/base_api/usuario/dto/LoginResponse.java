package com.example.base_api.usuario.dto;

import java.util.UUID;

public record LoginResponse(
        String token,
        String tipo,
        UUID usuarioId,
        String username,
        UUID pessoaId,
        UUID igrejaId,
        String igrejaTipo
) {
}
