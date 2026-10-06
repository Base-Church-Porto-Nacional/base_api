package com.example.base_api.aniversario.dto;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

/**
 * Aniversariante identificado para uma data, com a mensagem que será usada e a
 * indicação de já ter sido enviada (impede duplicidade).
 */
public record AniversarioPessoaResponse(
        UUID pessoaId,
        String nome,
        LocalDate dataNascimento,
        int idade,
        String mensagem,
        boolean enviado
) {

    public static int idade(LocalDate dataNascimento, LocalDate referencia) {
        if (dataNascimento == null) {
            return 0;
        }
        return Period.between(dataNascimento, referencia).getYears();
    }
}