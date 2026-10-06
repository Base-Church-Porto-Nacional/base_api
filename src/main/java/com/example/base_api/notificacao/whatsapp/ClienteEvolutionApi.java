package com.example.base_api.notificacao.whatsapp;

/**
 * Porta do cliente HTTP da Evolution API. Lança exceção em falha de envio para
 * que o registro de envio seja persistido com status FALHA.
 */
public interface ClienteEvolutionApi {

    void enviarTexto(String numero, String texto);
}