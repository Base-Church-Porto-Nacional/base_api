package com.example.base_api.notificacao;

/**
 * Indica que o canal de mensagem não tem transporte configurado e portanto não
 * houve entrega. Leva o registro de envio para FALHA em vez de ENVIADO.
 */
public class CanalNaoConfiguradoException extends RuntimeException {

    public CanalNaoConfiguradoException(String mensagem) {
        super(mensagem);
    }
}
