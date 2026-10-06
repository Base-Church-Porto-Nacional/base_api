package com.example.base_api.notificacao.whatsapp;

import java.util.Optional;

/**
 * Normaliza telefone brasileiro para o formato aceito pelo WhatsApp: DDI 55 +
 * DDD + número, apenas dígitos e sem o sinal de "+".
 */
public final class NormalizadorTelefoneWhatsapp {

    private static final String DDI_BRASIL = "55";

    private NormalizadorTelefoneWhatsapp() {
    }

    public static Optional<String> normalizar(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return Optional.empty();
        }
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.isEmpty()) {
            return Optional.empty();
        }
        if (digitos.startsWith("00")) {
            digitos = digitos.substring(2);
        }
        if (digitos.length() == 12 && digitos.startsWith("0") && !digitos.startsWith(DDI_BRASIL)) {
            digitos = digitos.substring(1);
        }
        if (digitos.length() == 10 || digitos.length() == 11) {
            return Optional.of(DDI_BRASIL + digitos);
        }
        if ((digitos.length() == 12 || digitos.length() == 13) && digitos.startsWith(DDI_BRASIL)) {
            return Optional.of(digitos);
        }
        return Optional.empty();
    }

    /**
     * Ofusca o número para registro em log, preservando apenas os 4 últimos dígitos.
     */
    public static String mascarar(String numero) {
        if (numero == null || numero.length() < 4) {
            return "****";
        }
        return "*".repeat(numero.length() - 4) + numero.substring(numero.length() - 4);
    }
}