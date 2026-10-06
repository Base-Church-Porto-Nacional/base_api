package com.example.base_api.aniversario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * {@code id} é aceito por compatibilidade com o front e ignorado: a mensagem é
 * única por igreja. Sem {@code igrejaId}, vale a igreja atual do usuário.
 */
public record MensagemAniversarioRequest(
        UUID id,

        UUID igrejaId,

        @NotBlank(message = "Informe a mensagem padrão")
        @Size(max = 2000, message = "Mensagem deve ter no máximo 2000 caracteres")
        String mensagem
) {
}
