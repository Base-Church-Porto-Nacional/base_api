package com.example.base_api.notificacao;

import com.example.base_api.aniversario.service.AniversarioEnvioStatusService;
import com.example.base_api.notificacao.evento.EventoMensagem;
import com.example.base_api.notificacao.infra.ConfiguracaoRabbit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consumidor dos eventos de mensagem: entrega ao canal e grava o desfecho no
 * registro de envio. Lança em falha para que o advice de retry do container
 * factory re-tente e, esgotadas as tentativas, publique na DLQ.
 */
@Component
@ConditionalOnProperty(prefix = "app.rabbit", name = "habilitado", havingValue = "true")
public class ConsumidorMensagem {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorMensagem.class);

    private final CanalMensagem canalMensagem;
    private final AniversarioEnvioStatusService statusService;

    public ConsumidorMensagem(CanalMensagem canalMensagem, AniversarioEnvioStatusService statusService) {
        this.canalMensagem = canalMensagem;
        this.statusService = statusService;
    }

    @RabbitListener(queues = ConfiguracaoRabbit.FILA_WHATSAPP)
    public void consumir(EventoMensagem evento) {
        log.info("Consumindo evento de mensagem (envioId={}).", evento.envioId());
        try {
            canalMensagem.enviar(new CanalMensagem.EnvioMensagem(
                    evento.pessoaId(), evento.nome(), evento.whatsapp(), evento.email(), evento.mensagem()
            ));
            statusService.registrarEnviado(evento.envioId());
        } catch (RuntimeException e) {
            statusService.registrarFalha(evento.envioId(), e);
            throw e;
        }
    }
}