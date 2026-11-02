package com.example.base_api.notificacao.config;

import com.example.base_api.notificacao.CanalMensagem;
import com.example.base_api.notificacao.CanalMensagemLog;
import com.example.base_api.notificacao.whatsapp.ClienteEvolutionApiRest;
import com.example.base_api.notificacao.whatsapp.CanalMensagemWhatsapp;
import com.example.base_api.notificacao.whatsapp.PropriedadesWhatsapp;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Seleciona o canal de mensagem: Evolution API quando o WhatsApp está habilitado,
 * ou um canal que falha explicitamente caso contrário — assim o registro de envio
 * fica FALHA em vez de ENVIADO quando nada foi entregue. Exatamente um
 * {@link CanalMensagem} fica registrado, o que evita ambiguidade de injeção.
 */
@Configuration
public class ConfiguracaoCanalMensagem {

    @Bean
    @ConditionalOnProperty(prefix = "app.whatsapp", name = "habilitado", havingValue = "true")
    public CanalMensagem canalWhatsapp(PropriedadesWhatsapp propriedades) {
        if (!propriedades.pronta()) {
            throw new IllegalStateException(
                    "app.whatsapp.habilitado=true exige urlBase, apiKey e instancia");
        }
        return new CanalMensagemWhatsapp(
                new ClienteEvolutionApiRest(restClientEvolution(propriedades), propriedades), propriedades);
    }

    /** RestClient da Evolution API com timeouts de conexão e leitura (app.whatsapp.*-timeout-ms). */
    static RestClient.Builder restClientEvolution(PropriedadesWhatsapp propriedades) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(propriedades.getConnectTimeoutMs()));
        fabrica.setReadTimeout(Duration.ofMillis(propriedades.getReadTimeoutMs()));
        return RestClient.builder().requestFactory(fabrica);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.whatsapp", name = "habilitado",
            havingValue = "false", matchIfMissing = true)
    public CanalMensagem canalLog() {
        return new CanalMensagemLog();
    }
}