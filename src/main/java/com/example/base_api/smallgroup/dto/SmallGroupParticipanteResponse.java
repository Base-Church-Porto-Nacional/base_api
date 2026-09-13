package com.example.base_api.smallgroup.dto;

import java.time.Instant;
import java.util.UUID;

public record SmallGroupParticipanteResponse(
        UUID pessoaId,
        String pessoaNome
) {
}