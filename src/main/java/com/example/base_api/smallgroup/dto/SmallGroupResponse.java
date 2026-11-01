package com.example.base_api.smallgroup.dto;

import com.example.base_api.common.entity.DiaSemana;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SmallGroupResponse(
        UUID id,
        String nome,
        UUID igrejaId,
        DiaSemana diaSemana,
        String horario,
        UUID liderPessoaId,
        String liderNome,
        UUID ministerioId,
        String ministerioNome,
        Boolean ativo,
        Integer nota,
        List<SmallGroupEncontroResponse> encontros,
        Instant createdAt,
        Instant updatedAt
) {
}