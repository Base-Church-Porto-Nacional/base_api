package com.example.base_api.smallgroup.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SmallGroupEncontroResponse(
        UUID id,
        UUID smallGroupId,
        LocalDate data,
        UUID anfitriaoPessoaId,
        String anfitriaoNome,
        List<SmallGroupParticipanteResponse> compareceram,
        String anotacoes,
        Instant createdAt,
        Instant updatedAt
) {
}