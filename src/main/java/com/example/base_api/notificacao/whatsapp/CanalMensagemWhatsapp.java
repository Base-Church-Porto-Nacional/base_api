package com.example.base_api.notificacao.whatsapp;

import com.example.base_api.notificacao.CanalMensagem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Canal de WhatsApp via Evolution API. Normaliza o número cadastrado no WhatsApp
 * do membro para DDI 55 + DDD (somente dígitos) e delega o envio ao cliente
 * HTTP. WhatsApp ausente ou em formato inválido é sinalizado como falha, para
 * ficar rastreável no registro de envio em vez de ser silenciosamente descartado.
 */
public class CanalMensagemWhatsapp implements CanalMensagem {

    private static final Logger log = LoggerFactory.getLogger(CanalMensagemWhatsapp.class);

    private final ClienteEvolutionApi clienteEvolutionApi;
    private final PropriedadesWhatsapp propriedades;

    public CanalMensagemWhatsapp(ClienteEvolutionApi clienteEvolutionApi, PropriedadesWhatsapp propriedades) {
        this.clienteEvolutionApi = clienteEvolutionApi;
        this.propriedades = propriedades;
    }

    @Override
    public void enviar(EnvioMensagem envio) {
        if (!propriedades.pronta()) {
            throw new CanalIndisponivelException(
                    "Canal WhatsApp desabilitado ou sem urlBase/apiKey/instancia configurados");
        }
        String numero = NormalizadorTelefoneWhatsapp.normalizar(envio.whatsapp())
                .orElseThrow(() -> new TelefoneInvalidoException(
                        "WhatsApp do membro não informado ou inválido: " + mascarado(envio.whatsapp())));
        clienteEvolutionApi.enviarTexto(numero, envio.mensagem());
    }

    private String mascarado(String whatsapp) {
        if (whatsapp == null || whatsapp.isBlank()) {
            return "não informado";
        }
        return NormalizadorTelefoneWhatsapp.mascarar(whatsapp.replaceAll("\\D", ""));
    }

    /** Canal não configurado: falha de infraestrutura, recuperável por configuração. */
    public static class CanalIndisponivelException extends RuntimeException {
        public CanalIndisponivelException(String message) {
            super(message);
        }
    }

    /** Telefone do membro ausente ou inválido: falha de dado, não recuperável pelo reenvio. */
    public static class TelefoneInvalidoException extends RuntimeException {
        public TelefoneInvalidoException(String message) {
            super(message);
        }
    }
}