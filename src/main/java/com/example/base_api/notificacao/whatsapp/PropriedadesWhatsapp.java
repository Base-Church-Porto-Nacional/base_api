package com.example.base_api.notificacao.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do canal WhatsApp (Evolution API). Sem estas propriedades o canal
 * fica indisponível e o envio é registrado como falha, sem quebrar o processamento
 * dos demais aniversariantes.
 */
@ConfigurationProperties(prefix = "app.whatsapp")
public class PropriedadesWhatsapp {

    private boolean habilitado = false;
    private String urlBase = "";
    private String apiKey = "";
    private String instancia = "";
    private int delayMs = 1200;
    private boolean linkPreview = true;

    public boolean isHabilitado() {
        return habilitado;
    }

    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    public String getUrlBase() {
        return urlBase;
    }

    public void setUrlBase(String urlBase) {
        this.urlBase = urlBase;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getInstancia() {
        return instancia;
    }

    public void setInstancia(String instancia) {
        this.instancia = instancia;
    }

    public int getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(int delayMs) {
        this.delayMs = delayMs;
    }

    public boolean isLinkPreview() {
        return linkPreview;
    }

    public void setLinkPreview(boolean linkPreview) {
        this.linkPreview = linkPreview;
    }

    public boolean pronta() {
        return habilitado
                && naoVazio(urlBase)
                && naoVazio(apiKey)
                && naoVazio(instancia);
    }

    public String urlBaseNormalizada() {
        if (urlBase == null || urlBase.isBlank()) {
            return "";
        }
        String base = urlBase.strip();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private boolean naoVazio(String valor) {
        return valor != null && !valor.isBlank();
    }
}