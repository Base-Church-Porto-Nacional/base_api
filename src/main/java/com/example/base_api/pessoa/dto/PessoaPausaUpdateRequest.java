package com.example.base_api.pessoa.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PessoaPausaUpdateRequest(
        @NotNull(message = "dataInicio é obrigatória")
        LocalDate dataInicio,

        LocalDate dataFim,

        @Size(max = 255, message = "Motivo deve ter no máximo 255 caracteres")
        String motivo,

        String observacoes,

        @NotNull(message = "Campo ativa é obrigatório")
        Boolean ativa
) {
}
