package com.example.base_api.notificacao.despacho;

import com.example.base_api.aniversario.entity.AniversarioEnvio;
import com.example.base_api.aniversario.entity.StatusEnvioAniversario;
import com.example.base_api.pessoa.entity.Pessoa;

/**
 * Despacha a mensagem de um registro de envio, seja para entrega síncrona
 * direta, seja para publicação no RabbitMQ.
 */
public interface DespachadorMensagem {

    Resultado despachar(AniversarioEnvio envio, Pessoa pessoa, String mensagem);

    /**
     * Desfecho do despacho. Com Rabbit o registro permanece PENDENTE, pois quem
     * entrega e grava o status final é o consumidor.
     */
    record Resultado(StatusEnvioAniversario status, String erro) {

        public static Resultado enviado() {
            return new Resultado(StatusEnvioAniversario.ENVIADO, null);
        }

        public static Resultado enfileirado() {
            return new Resultado(StatusEnvioAniversario.PENDENTE, null);
        }

        public static Resultado falha(String erro) {
            return new Resultado(StatusEnvioAniversario.FALHA, erro);
        }

        public static Resultado falhaDe(Exception e) {
            String mensagem = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return falha(mensagem);
        }
    }
}