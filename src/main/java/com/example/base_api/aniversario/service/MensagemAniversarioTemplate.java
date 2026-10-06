package com.example.base_api.aniversario.service;

import com.example.base_api.aniversario.config.MensagemAniversarioProperties;
import com.example.base_api.pessoa.entity.Pessoa;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

/**
 * Renderiza o template configurável da mensagem de aniversário substituindo os
 * marcadores {nome}, {primeiroNome}, {idade} e {igreja}. Também aceita os
 * marcadores inseridos pela tela de configuração do front: [Nome] (primeiro
 * nome), [Nome Completo], [Idade] e [Igreja]. Marcadores não reconhecidos são
 * mantidos como texto.
 */
@Component
public class MensagemAniversarioTemplate {

    public String renderizar(Pessoa pessoa, String template, String nomeIgreja, LocalDate dataAniversario) {
        String padrao = new MensagemAniversarioProperties().getTemplate();
        String base = Optional.ofNullable(template)
                .filter(t -> !t.isBlank())
                .orElse(padrao);

        String primeiroNome = primeiroNome(pessoa);
        String nome = Optional.ofNullable(pessoa.getNome()).orElse("");
        String idade = String.valueOf(idade(pessoa, dataAniversario));
        String igreja = Optional.ofNullable(nomeIgreja).orElse("");
        return base
                .replace("{primeiroNome}", primeiroNome)
                .replace("{nome}", nome)
                .replace("{idade}", idade)
                .replace("{igreja}", igreja)
                .replace("[Nome Completo]", nome)
                .replace("[Nome]", primeiroNome)
                .replace("[Idade]", idade)
                .replace("[Igreja]", igreja);
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