package com.example.base_api.notificacao.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Cliente da Evolution API. Diferente do padrão fire-and-forget, as exceções do
 * RestClient NÃO são engolidas aqui: elas sobem para o chamador, que as converte
 * em status FALHA no registro de envio — é o que permite rastrear a falha.
 */
public class ClienteEvolutionApiRest implements ClienteEvolutionApi {

    private static final Logger log = LoggerFactory.getLogger(ClienteEvolutionApiRest.class);

    private final RestClient restClient;
    private final PropriedadesWhatsapp propriedades;

    public ClienteEvolutionApiRest(RestClient.Builder builder, PropriedadesWhatsapp propriedades) {
        this.restClient = builder.build();
        this.propriedades = propriedades;
    }

    @Override
    public void enviarTexto(String numero, String texto) {
        restClient
                .post()
                .uri(propriedades.urlBaseNormalizada() + "/message/sendText/{instancia}",
                        propriedades.getInstancia().strip())
                .contentType(MediaType.APPLICATION_JSON)
                .header("apikey", propriedades.getApiKey())
                .body(new RequisicaoEnvioTextoEvolution(
                        numero, texto, propriedades.getDelayMs(), propriedades.isLinkPreview()))
                .retrieve()
                .toBodilessEntity();
        log.info("Mensagem WhatsApp enviada via Evolution API para {}.",
                NormalizadorTelefoneWhatsapp.mascarar(numero));
    }
}