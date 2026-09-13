package com.example.base_api.smallgroup.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SmallGroupEncontroRequest(
        /** Referência ao grupo (presente também no body do PUT, vindo do app). */
        UUID smallGroupId,

        @NotNull(message = "Data do encontro é obrigatória")
        LocalDate data,

        UUID anfitriaoPessoaId,

        List<SmallGroupParticipanteRequest> compareceram,

        String anotacoes
) {
}