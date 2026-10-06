package com.example.base_api.notificacao.evento;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato de evento de notificação publicado no RabbitMQ e consumido pelo
 * listener de mensagens. A mensagem é autocontida: o produtor resolve
 * destinatário e conteúdo, de modo que o consumidor não conhece o domínio.
 */
public record EventoMensagem(
        String tipo,
        Canal canal,
        UUID pessoaId,
        String nome,
        String whatsapp,
        String email,
        String mensagem,
        UUID envioId,
        Instant criadoEm
) {

    /** Exchange direto em que os eventos são publicados; filas e bindings são declarados aqui. */
    public static final String EXCHANGE = "igreja.notificacoes";
    public static final String CHAVE_WHATSAPP = "canal.whatsapp";
    public static final String CHAVE_ERRO_WHATSAPP = "erro.canal.whatsapp";

    public enum Canal {
        WHATSAPP(EventoMensagem.CHAVE_WHATSAPP);

        private final String chave;

        Canal(String chave) {
            this.chave = chave;
        }

        public String chave() {
            return chave;
        }
    }

    public static EventoMensagem whatsapp(
            UUID pessoaId,
            String nome,
            String whatsapp,
            String email,
            String mensagem,
            UUID envioId
    ) {
        return new EventoMensagem(
                "MENSAGEM_WHATSAPP", Canal.WHATSAPP, pessoaId, nome, whatsapp, email,
                mensagem, envioId, Instant.now());
    }
}