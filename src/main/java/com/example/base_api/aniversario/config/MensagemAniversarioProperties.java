package com.example.base_api.aniversario.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração da mensagem de feliz aniversário. O texto é um template com os
 * marcadores {nome}, {primeiroNome}, {idade} e {igreja}. Alterar o texto é feito
 * apenas por configuração (env MESSAGE_ANIVERSARIO_TEMPLATE), sem tocar na regra
 * de negócio.
 */
@ConfigurationProperties(prefix = "app.aniversario")
public class MensagemAniversarioProperties {

    private boolean ativo = true;
    private String template = "Feliz aniversário, {primeiroNome}! Que Deus abençoe a sua vida "
            + "e lhe conceda um dia cheio de paz, alegria e gratidão. Com carinho, {igreja}.";
    private String cron = "0 0 12 * * *";

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }
}