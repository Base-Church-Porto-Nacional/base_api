package com.example.base_api.escala.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GeracaoEscalaResponse(
        UUID escalaId,
        String status,
        String message,
        List<SugestaoItem> sugestoes
) {
    public record SugestaoItem(
            UUID servicoId,
            String servicoNome,
            UUID pessoaId,
            String pessoaNome,
            LocalDate data,
            UUID eventoId,
            String motivo
    ) {
    }
}