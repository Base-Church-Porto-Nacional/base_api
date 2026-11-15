package com.example.base_api.notificacao;

import com.example.base_api.notificacao.whatsapp.NormalizadorTelefoneWhatsapp;

/**
 * Canal sem transporte real: registra a mensagem e falha explicitamente.
 *
 * <p>Existe só para o perfil {@code dev}, onde não há Evolution API. Ele lança
 * exceção de propósito — registrar a mensagem em log não é entrega, e marcar o
 * envio como ENVIADO seria mentir sobre um contato que ninguém recebeu. O
 * registro fica FALHA com o motivo visível, que é a informação útil.</p>
 */
public class CanalMensagemLog implements CanalMensagem {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CanalMensagemLog.class);

    @Override
    public void enviar(EnvioMensagem envio) {
        // Nunca registrar o corpo: pode conter link de uso único (convite/redefinição de senha).
        String numero = envio.whatsapp() == null ? null
                : NormalizadorTelefoneWhatsapp.mascarar(envio.whatsapp().replaceAll("\\D", ""));
        log.info("mensagem não entregue (canal de log) pessoaId={} whatsapp={} tamanho={}",
                envio.pessoaId(), numero, envio.mensagem() == null ? 0 : envio.mensagem().length());
        throw new CanalNaoConfiguradoException(
                "WhatsApp não configurado: mensagem apenas registrada em log, não entregue. "
                        + "Defina app.whatsapp.habilitado=true com urlBase, apiKey e instancia.");
    }
}
