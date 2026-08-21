package com.example.base_api.jornada.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PessoaEtapaRequest(
        @NotNull(message = "etapaId é obrigatório")
        java.util.UUID etapaId,

        @NotNull(message = "concluida é obrigatório")
        Boolean concluida,

        LocalDate dataConclusao,
        String observacoes
) {
}
