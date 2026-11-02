package com.example.base_api.security;

import com.example.base_api.notificacao.whatsapp.NormalizadorTelefoneWhatsapp;

import java.util.Locale;
import java.util.Optional;

/**
 * Identificador de login: e-mail (contém "@", comparado em minúsculas) ou telefone
 * (normalizado como o WhatsApp, para que formatos diferentes caiam na mesma conta).
 */
public final class Identificador {

    private Identificador() {
    }

    public static Optional<String> email(String bruto) {
        if (bruto == null || !bruto.contains("@")) {
            return Optional.empty();
        }
        String email = bruto.trim().toLowerCase(Locale.ROOT);
        return email.isEmpty() ? Optional.empty() : Optional.of(email);
    }

    public static Optional<String> telefone(String bruto) {
        if (bruto == null || bruto.contains("@")) {
            return Optional.empty();
        }
        return NormalizadorTelefoneWhatsapp.normalizar(bruto);
    }

    /** Forma canônica (e-mail em minúsculas ou telefone normalizado); o texto aparado quando não reconhecido. */
    public static String normalizado(String bruto) {
        if (bruto == null) {
            return "";
        }
        return email(bruto).or(() -> telefone(bruto)).orElse(bruto.trim().toLowerCase(Locale.ROOT));
    }
}
