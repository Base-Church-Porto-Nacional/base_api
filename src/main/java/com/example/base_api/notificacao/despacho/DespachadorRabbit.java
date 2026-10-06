package com.example.base_api.notificacao.despacho;

import com.example.base_api.aniversario.entity.AniversarioEnvio;
import com.example.base_api.notificacao.evento.EventoMensagem;
import com.example.base_api.notificacao.produtor.PublicadorMensagem;
import com.example.base_api.pessoa.entity.Pessoa;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Despacho assíncrono: publica o evento e devolve o registro como PENDENTE. O
 * consumidor entrega e grava ENVIADO/FALHA; falhas de canal são reprocessadas
 * pelo retry do listener e, esgotadas as tentativas, vão para a DLQ.
 */
@Component
@ConditionalOnProperty(prefix = "app.rabbit", name = "habilitado", havingValue = "true")
@ConditionalOnBean(PublicadorMensagem.class)
public class DespachadorRabbit implements DespachadorMensagem {

    private final PublicadorMensagem publicador;

    public DespachadorRabbit(PublicadorMensagem publicador) {
        this.publicador = publicador;
    }

    @Override
    public Resultado despachar(AniversarioEnvio envio, Pessoa pessoa, String mensagem) {
        try {
            publicador.publicar(EventoMensagem.whatsapp(
                    pessoa.getId(), pessoa.getNome(), pessoa.getWhatsapp(), pessoa.getEmail(),
                    mensagem, envio.getId()
            ));
            return Resultado.enfileirado();
        } catch (RuntimeException e) {
            return Resultado.falhaDe(e);
        }
    }
}