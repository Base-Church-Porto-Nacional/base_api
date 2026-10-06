package com.example.base_api.notificacao.despacho;

import com.example.base_api.aniversario.entity.AniversarioEnvio;
import com.example.base_api.notificacao.CanalMensagem;
import com.example.base_api.pessoa.entity.Pessoa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Despacho síncrono: entrega direto ao canal e resolve o status na mesma
 * chamada. É o caminho padrão quando o RabbitMQ está desligado.
 */
@Component
@ConditionalOnProperty(prefix = "app.rabbit", name = "habilitado",
        havingValue = "false", matchIfMissing = true)
public class DespachadorSincrono implements DespachadorMensagem {

    private static final Logger log = LoggerFactory.getLogger(DespachadorSincrono.class);

    private final CanalMensagem canalMensagem;

    public DespachadorSincrono(CanalMensagem canalMensagem) {
        this.canalMensagem = canalMensagem;
    }

    @Override
    public Resultado despachar(AniversarioEnvio envio, Pessoa pessoa, String mensagem) {
        try {
            canalMensagem.enviar(new CanalMensagem.EnvioMensagem(
                    pessoa.getId(), pessoa.getNome(), pessoa.getWhatsapp(), pessoa.getEmail(), mensagem
            ));
            return Resultado.enviado();
        } catch (RuntimeException e) {
            log.warn("falha no envio síncrono para pessoa={}: {}", pessoa.getId(), e.getMessage());
            return Resultado.falhaDe(e);
        }
    }
}