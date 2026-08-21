package com.example.base_api.jornada.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record JornadaResponse(
        UUID id,
        UUID pessoaId,
        Boolean aceitouJesus,
        LocalDate dataAceitouJesus,
        Boolean batizado,
        LocalDate dataBatismo,
        Boolean concluiuTrilhaBase,
        LocalDate dataConclusaoTrilhaBase,
        Boolean membro,
        LocalDate dataMembro,
        Instant createdAt,
        Instant updatedAt
) {
}
