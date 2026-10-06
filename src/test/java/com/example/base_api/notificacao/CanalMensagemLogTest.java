package com.example.base_api.notificacao;

import com.example.base_api.notificacao.whatsapp.NormalizadorTelefoneWhatsapp;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CanalMensagemLogTest {

    @Test
    void deveFalharExplicitamentePorqueLogNaoEEntrega() {
        CanalMensagemLog canal = new CanalMensagemLog();

        assertThatThrownBy(() -> canal.enviar(new CanalMensagem.EnvioMensagem(
                UUID.randomUUID(), "Maria", "5511999994444", null, "Feliz aniversário!")))
                .isInstanceOf(CanalNaoConfiguradoException.class)
                .hasMessageContaining("não entregue");
    }

    @Test
    void numeroInvalidoContinuaSendoRejeitadoPelaNormalizacao() {
        assertThat(NormalizadorTelefoneWhatsapp.normalizar("11988")).isEmpty();
        assertThat(NormalizadorTelefoneWhatsapp.normalizar("(11) 99999-4444"))
                .contains("5511999994444");
    }
}
