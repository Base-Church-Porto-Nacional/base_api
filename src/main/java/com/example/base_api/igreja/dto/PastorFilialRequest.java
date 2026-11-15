package com.example.base_api.igreja.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Pastor da nova filial: exatamente um dos modos — {@code pessoaId} (pessoa existente, transferida para a
 * filial) ou {@code nome} (pessoa nova, com e-mail ou WhatsApp para receber o convite).
 */
public record PastorFilialRequest(
        UUID pessoaId,

        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nome,

        @Email(message = "E-mail inválido")
        String email,

        @Size(max = 30, message = "WhatsApp deve ter no máximo 30 caracteres")
        String whatsapp
) {
}
