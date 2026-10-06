package com.example.base_api.notificacao.whatsapp;

/**
 * Corpo do sendText da Evolution v2: campo {@code text} no topo (v2.3+).
 */
public record RequisicaoEnvioTextoEvolution(String number, String text, int delay, boolean linkPreview) {
}