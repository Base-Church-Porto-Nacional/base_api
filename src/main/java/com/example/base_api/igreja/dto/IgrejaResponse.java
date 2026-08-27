package com.example.base_api.igreja.dto;

import com.example.base_api.igreja.entity.IgrejaTipo;

import java.time.Instant;
import java.util.UUID;

public record IgrejaResponse(
        UUID id,
        String nome,
        IgrejaTipo tipo,
        UUID igrejaMatrizId,
        UUID pastorPessoaId,
        String pastorNome,
        UUID adminUsuarioId,
        String adminNome,
        Boolean ativo,
        Instant createdAt,
        Instant updatedAt
) {
}
