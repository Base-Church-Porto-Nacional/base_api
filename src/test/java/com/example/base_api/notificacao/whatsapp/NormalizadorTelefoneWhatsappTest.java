package com.example.base_api.notificacao.whatsapp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizadorTelefoneWhatsappTest {

    @Test
    void deveAceitarCelularComDddESemDdi() {
        assertThat(NormalizadorTelefoneWhatsapp.normalizar("11988887777"))
                .contains("5511988887777");
    }

    @Test
    void deveAceitarCelularComMascaras() {
        assertThat(NormalizadorTelefoneWhatsapp.normalizar("(11) 98888-7777"))
                .contains("5511988887777");
    }

    @Test
    void deveAceitarNumeroJaComDdi() {
        assertThat(NormalizadorTelefoneWhatsapp.normalizar("+55 11 98888-7777"))
                .contains("5511988887777");
    }

    @Test
    void deveAceitarNumeroComPrefixoDeSaidaInternacional() {
        assertThat(NormalizadorTelefoneWhatsapp.normalizar("0011988887777"))
                .contains("5511988887777");
    }

    @Test
    void deveAceitarNumeroDeOnzeDigitosJaComDdi() {
        assertThat(NormalizadorTelefoneWhatsapp.normalizar("5511988887777"))
                .contains("5511988887777");
    }

    @Test
    void deveAceitarTelefoneFixoDeDezDigitos() {
        assertThat(NormalizadorTelefoneWhatsapp.normalizar("1133334444"))
                .contains("551133334444");
    }

    @Test
    void deveRejeitarNumeroDeDozeDigitosComZeroDeAcessoNacional() {
        assertThat(NormalizadorTelefoneWhatsapp.normalizar("0211988887777")).isEmpty();
    }

    @Test
    void deveRejeitarPrefixoDeSaidaInternacionalSobreNumeroJaCompleto() {
        assertThat(NormalizadorTelefoneWhatsapp.normalizar("0095511988887777")).isEmpty();
    }

    @Test
    void deveRejeitarTelefoneAusenteOuVazio() {
        assertThat(NormalizadorTelefoneWhatsapp.normalizar(null)).isEmpty();
        assertThat(NormalizadorTelefoneWhatsapp.normalizar("")).isEmpty();
        assertThat(NormalizadorTelefoneWhatsapp.normalizar("   ")).isEmpty();
    }

    @Test
    void deveRejeitarNumeroSemDdd() {
        assertThat(NormalizadorTelefoneWhatsapp.normalizar("988887777")).isEmpty();
    }

    @Test
    void deveRejeitarNumeroCurtoDemais() {
        assertThat(NormalizadorTelefoneWhatsapp.normalizar("123")).isEmpty();
    }

    /**
     * O normalizador trata qualquer sequência de 10 ou 11 dígitos como número
     * brasileiro e prefixa o DDI 55. Um número estrangeiro digitado sem o DDI
     * errado acabaria Recebendo o 55 — comportamento herdado do serviço de
     * referência, documentado aqui para não ser surprise em produção.
     */
    @Test
    void deveConsiderarBrasileiroQualquerNumeroDeDezOuOnzeDigitos() {
        assertThat(NormalizadorTelefoneWhatsapp.normalizar("14155552671"))
                .contains("5514155552671");
    }

    @Test
    void deveMascararNumeroPreservandoQuatroUltimosDigitos() {
        assertThat(NormalizadorTelefoneWhatsapp.mascarar("5511988887777"))
                .isEqualTo("*********7777");
    }

    @Test
    void deveMascararNumeroCurtoSemVazarDigitos() {
        assertThat(NormalizadorTelefoneWhatsapp.mascarar("12")).isEqualTo("****");
        assertThat(NormalizadorTelefoneWhatsapp.mascarar(null)).isEqualTo("****");
    }
}