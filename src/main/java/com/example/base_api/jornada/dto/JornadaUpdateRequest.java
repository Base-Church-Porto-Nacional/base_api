package com.example.base_api.jornada.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record JornadaUpdateRequest(
        @NotNull(message = "aceitouJesus é obrigatório")
        Boolean aceitouJesus,
        LocalDate dataAceitouJesus,

        @NotNull(message = "batizado é obrigatório")
        Boolean batizado,
        LocalDate dataBatismo,

        @NotNull(message = "concluiuTrilhaBase é obrigatório")
        Boolean concluiuTrilhaBase,
        LocalDate dataConclusaoTrilhaBase,

        @NotNull(message = "membro é obrigatório")
        Boolean membro,
        LocalDate dataMembro
) {
}
