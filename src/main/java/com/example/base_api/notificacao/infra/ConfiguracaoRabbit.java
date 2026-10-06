package com.example.base_api.notificacao.infra;

import com.example.base_api.notificacao.evento.EventoMensagem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;

import java.time.Duration;

/**
 * Infraestrutura RabbitMQ: exchange direto {@code igreja.notificacoes} com uma
 * fila por canal e uma dead letter por canal. O listener tem retry com
 * republicação na DLQ — é o que dá recuperabilidade quando o canal falha.
 *
 * <p>Registrado apenas quando {@code app.rabbit.habilitado=true}; sem isso o
 * envio de mensagem acontece de forma síncrona, sem broker.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "app.rabbit", name = "habilitado", havingValue = "true")
public class ConfiguracaoRabbit {

    public static final String FILA_WHATSAPP = "notificacao.whatsapp";
    public static final String FILA_DLQ_WHATSAPP = "notificacao.whatsapp.dlq";
    public static final String EXCHANGE_ERROS = "igreja.notificacoes.erros";

    private static final int MAX_TENTATIVAS = 3;
    private static final long INTERVALO_MS = 1000L;
    private static final long INTERVALO_MAXIMO_MS = 10000L;

    @Bean
    DirectExchange exchangeNotificacoes() {
        return new DirectExchange(EventoMensagem.EXCHANGE, true, false);
    }

    @Bean
    DirectExchange exchangeErros() {
        return new DirectExchange(EXCHANGE_ERROS, true, false);
    }

    @Bean
    Queue filaWhatsapp() {
        return QueueBuilder.durable(FILA_WHATSAPP)
                .deadLetterExchange(EXCHANGE_ERROS)
                .deadLetterRoutingKey(EventoMensagem.CHAVE_ERRO_WHATSAPP)
                .build();
    }

    @Bean
    Queue filaDlqWhatsapp() {
        return QueueBuilder.durable(FILA_DLQ_WHATSAPP).build();
    }

    @Bean
    Binding vinculoWhatsapp(DirectExchange exchangeNotificacoes, Queue filaWhatsapp) {
        return BindingBuilder.bind(filaWhatsapp)
                .to(exchangeNotificacoes).with(EventoMensagem.CHAVE_WHATSAPP);
    }

    @Bean
    Binding vinculoDlqWhatsapp(DirectExchange exchangeErros, Queue filaDlqWhatsapp) {
        return BindingBuilder.bind(filaDlqWhatsapp)
                .to(exchangeErros).with(EventoMensagem.CHAVE_ERRO_WHATSAPP);
    }

    @Bean
    MessageConverter conversorMensagem() {
        ObjectMapper mapeador = new ObjectMapper();
        mapeador.registerModule(new JavaTimeModule());
        mapeador.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapeador);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter conversor) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(conversor);
        return template;
    }

    /**
     * Listener com retry e republicação na DLQ após as tentativas. O recoverer
     * está ligado de fato ao advice — uma bean de recoverer sozinha é inerte.
     */
    @Bean
    SimpleRabbitListenerContainerFactory containerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter conversor,
            RabbitTemplate rabbitTemplate
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(conversor);
        factory.setDefaultRequeueRejected(false);

        RepublishMessageRecoverer recoverer =
                new RepublishMessageRecoverer(rabbitTemplate, EXCHANGE_ERROS, EventoMensagem.CHAVE_ERRO_WHATSAPP);

        // maxRetries conta novas tentativas: MAX_TENTATIVAS - 1 retries = 3 envios.
        RetryPolicy politica = RetryPolicy.builder()
                .maxRetries(MAX_TENTATIVAS - 1L)
                .delay(Duration.ofMillis(INTERVALO_MS))
                .multiplier(2.0)
                .maxDelay(Duration.ofMillis(INTERVALO_MAXIMO_MS))
                .build();

        Advice advice = org.springframework.amqp.rabbit.config.RetryInterceptorBuilder
                .stateless()
                .retryPolicy(politica)
                .recoverer(recoverer)
                .build();
        factory.setAdviceChain(advice);
        return factory;
    }
}