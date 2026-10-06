package com.example.base_api.notificacao;

import java.util.UUID;

/**
 * Porta de integração com o serviço/canal responsável pelo envio da mensagem.
 * A implementação padrão (log) permite rodar sem canal configurado; o canal
 * WhatsApp/Evolution é plugado por configuração.
 */
public interface CanalMensagem {

    /**
     * Envia a mensagem ao destinatário. Lança exceção para sinalizar falha, que
     * é convertida em status FALHA no registro de envio.
     */
    void enviar(EnvioMensagem envio);

    record EnvioMensagem(
            UUID pessoaId,
            String nome,
            String whatsapp,
            String email,
            String mensagem
    ) {
    }
}