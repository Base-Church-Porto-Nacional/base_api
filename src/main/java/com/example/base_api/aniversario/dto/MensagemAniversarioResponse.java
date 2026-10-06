package com.example.base_api.aniversario.dto;

import java.util.UUID;

/**
 * Mensagem efetiva da igreja. {@code id} nulo indica que a igreja ainda não tem
 * mensagem própria e {@code mensagem} é a herdada (matriz ou configuração).
 */
public record MensagemAniversarioResponse(
        UUID id,
        UUID igrejaId,
        String mensagem
) {
}
