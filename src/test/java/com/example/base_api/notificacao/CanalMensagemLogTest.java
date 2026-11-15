package com.example.base_api.notificacao;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.base_api.notificacao.CanalMensagem.EnvioMensagem;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CanalMensagemLogTest {

    @Test
    void naoRegistraCorpoDaMensagem() {
        Logger logger = (Logger) LoggerFactory.getLogger(CanalMensagemLog.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            EnvioMensagem envio = new EnvioMensagem(UUID.randomUUID(), "Maria", "5563999990000", null,
                    "Crie sua senha: http://app/acesso/SEGREDO123");

            assertThatThrownBy(() -> new CanalMensagemLog().enviar(envio))
                    .isInstanceOf(CanalNaoConfiguradoException.class);

            assertThat(appender.list).isNotEmpty();
            assertThat(appender.list).allSatisfy(e -> {
                assertThat(e.getFormattedMessage()).doesNotContain("SEGREDO123").doesNotContain("acesso/");
                assertThat(e.getFormattedMessage()).doesNotContain("5563999990000");
            });
        } finally {
            logger.detachAppender(appender);
        }
    }
}
