package com.example.base_api.notificacao.config;

import com.example.base_api.notificacao.whatsapp.ClienteEvolutionApiRest;
import com.example.base_api.notificacao.whatsapp.PropriedadesWhatsapp;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.web.client.ResourceAccessException;

import java.net.ServerSocket;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class ConfiguracaoCanalMensagemTest {

    @Test
    void timeoutsPadraoSaoCincoEDezSegundos() {
        PropriedadesWhatsapp propriedades = new PropriedadesWhatsapp();

        assertThat(propriedades.getConnectTimeoutMs()).isEqualTo(5000);
        assertThat(propriedades.getReadTimeoutMs()).isEqualTo(10000);
    }

    @Test
    void timeoutsSaoConfiguraveisPorPropriedade() {
        Binder binder = new Binder(new MapConfigurationPropertySource(Map.of(
                "app.whatsapp.connect-timeout-ms", "1500",
                "app.whatsapp.read-timeout-ms", "2500")));

        PropriedadesWhatsapp propriedades = binder.bind("app.whatsapp", PropriedadesWhatsapp.class).get();

        assertThat(propriedades.getConnectTimeoutMs()).isEqualTo(1500);
        assertThat(propriedades.getReadTimeoutMs()).isEqualTo(2500);
    }

    @Test
    void evolutionQueNaoRespondeEstouraOReadTimeout() throws Exception {
        try (ServerSocket mudo = new ServerSocket(0)) {
            PropriedadesWhatsapp propriedades = new PropriedadesWhatsapp();
            propriedades.setHabilitado(true);
            propriedades.setUrlBase("http://127.0.0.1:" + mudo.getLocalPort());
            propriedades.setApiKey("chave");
            propriedades.setInstancia("igreja");
            propriedades.setReadTimeoutMs(300);
            ClienteEvolutionApiRest cliente = new ClienteEvolutionApiRest(
                    ConfiguracaoCanalMensagem.restClientEvolution(propriedades), propriedades);

            assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                    assertThatThrownBy(() -> cliente.enviarTexto("5511999999999", "oi"))
                            .isInstanceOf(ResourceAccessException.class));
        }
    }
}
