package com.example.base_api.notificacao.produtor;

import com.example.base_api.notificacao.evento.EventoMensagem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Publica o evento de mensagem no exchange. Falha de publicação é propagada ao
 * chamador, que a registra como FALHA no envio — não é engolida, senão o
 * registro marcaria sucesso sem que nada tenha sido enfileirado.
 */
@Component
@ConditionalOnProperty(prefix = "app.rabbit", name = "habilitado", havingValue = "true")
public class PublicadorMensagem {

    private static final Logger log = LoggerFactory.getLogger(PublicadorMensagem.class);

    private final RabbitTemplate rabbitTemplate;

    public PublicadorMensagem(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicar(EventoMensagem evento) {
        // O conversor define o content type e o header de tipo a partir do
        // payload, o que permite ao consumidor desserializar direto no record.
        rabbitTemplate.convertAndSend(EventoMensagem.EXCHANGE, evento.canal().chave(), evento);
        log.info("Evento de mensagem publicado (canal={}, envioId={}).",
                evento.canal(), evento.envioId());
    }
}