package com.example.base_api.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MeUpdateRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nome,

        @Size(max = 150, message = "Nome social deve ter no máximo 150 caracteres")
        String nomeSocial,

        /** Opcional se a conta entra pelo telefone; ao menos um entre e-mail e telefone deve restar. */
        @Email(message = "E-mail inválido")
        String email,

        @Size(max = 30, message = "Telefone deve ter no máximo 30 caracteres")
        String telefone,

        LocalDate dataNascimento,

        String observacoes,

        @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres")
        String novaSenha,

        /** Obrigatória quando novaSenha é informada ou quando o e-mail/telefone de login muda. */
        String senhaAtual
) {
}
