package com.example.base_api.aniversario.service;

import com.example.base_api.aniversario.config.MensagemAniversarioProperties;
import com.example.base_api.pessoa.entity.Pessoa;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

/**
 * Renderiza o template configurável da mensagem de aniversário substituindo os
 * marcadores {nome}, {primeiroNome}, {idade} e {igreja}. Marcadores não
 * reconhecidos são mantidos como texto.
 */
@Component
public class MensagemAniversarioTemplate {

    public String renderizar(Pessoa pessoa, String template, String nomeIgreja, LocalDate dataAniversario) {
        String padrao = new MensagemAniversarioProperties().getTemplate();
        String base = Optional.ofNullable(template)
                .filter(t -> !t.isBlank())
                .orElse(padrao);

        return base
                .replace("{primeiroNome}", primeiroNome(pessoa))
                .replace("{nome}", Optional.ofNullable(pessoa.getNome()).orElse(""))
                .replace("{idade}", String.valueOf(idade(pessoa, dataAniversario)))
                .replace("{igreja}", Optional.ofNullable(nomeIgreja).orElse(""));
    }

    private String primeiroNome(Pessoa pessoa) {
        String nome = Optional.ofNullable(pessoa.getNome()).orElse("").trim();
        if (nome.isEmpty()) {
            return "";
        }
        return nome.split("\\s+")[0];
    }

    private int idade(Pessoa pessoa, LocalDate dataAniversario) {
        if (pessoa.getDataNascimento() == null) {
            return 0;
        }
        return Period.between(pessoa.getDataNascimento(), dataAniversario).getYears();
    }
}