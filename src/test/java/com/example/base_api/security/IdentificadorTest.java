package com.example.base_api.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdentificadorTest {

    @Test
    void emailNormalizaParaMinusculas() {
        assertThat(Identificador.email(" Maria@Base.COM ")).contains("maria@base.com");
        assertThat(Identificador.telefone(" Maria@Base.COM ")).isEmpty();
    }

    @Test
    void telefoneEmFormatosDiferentesNormalizaIgual() {
        assertThat(Identificador.telefone("(63) 99999-0000")).contains("5563999990000");
        assertThat(Identificador.telefone("63999990000")).contains("5563999990000");
        assertThat(Identificador.telefone("+55 63 99999-0000")).contains("5563999990000");
    }

    @Test
    void emailNaoViraTelefone() {
        assertThat(Identificador.telefone("63999990000@base.com")).isEmpty();
        assertThat(Identificador.email("63999990000@base.com")).contains("63999990000@base.com");
        assertThat(Identificador.email("63999990000")).isEmpty();
    }

    @Test
    void vazioOuNuloNaoIdentifica() {
        assertThat(Identificador.email(null)).isEmpty();
        assertThat(Identificador.telefone(null)).isEmpty();
        assertThat(Identificador.email("   ")).isEmpty();
        assertThat(Identificador.telefone("abc")).isEmpty();
    }
}
