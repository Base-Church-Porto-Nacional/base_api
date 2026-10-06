package com.example.base_api.notificacao.whatsapp;

import com.example.base_api.notificacao.CanalMensagem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CanalMensagemWhatsappTest {

    @Mock
    private ClienteEvolutionApi clienteEvolutionApi;

    private PropriedadesWhatsapp propriedades;
    private CanalMensagemWhatsapp canal;

    @BeforeEach
    void setUp() {
        propriedades = new PropriedadesWhatsapp();
        canal = new CanalMensagemWhatsapp(clienteEvolutionApi, propriedades);
    }

    private CanalMensagem.EnvioMensagem envio(String whatsapp) {
        return new CanalMensagem.EnvioMensagem(
                UUID.randomUUID(), "Maria", whatsapp, "maria@email.com", "Feliz aniversário, Maria!");
    }

    private void habilitarCanalCompleto() {
        propriedades.setHabilitado(true);
        propriedades.setUrlBase("http://localhost:8080");
        propriedades.setApiKey("chave-secreta");
        propriedades.setInstancia("instancia-teste");
    }

    @Test
    void deveEnviarMensagemComWhatsappNormalizado() {
        habilitarCanalCompleto();

        canal.enviar(envio("(11) 98888-7777"));

        verify(clienteEvolutionApi).enviarTexto("5511988887777", "Feliz aniversário, Maria!");
    }

    @Test
    void deveFalharDeFormaRastreavelQuandoOCanalNaoEstaConfigurado() {
        CanalMensagem.EnvioMensagem envio = envio("11988887777");

        assertThatThrownBy(() -> canal.enviar(envio))
                .isInstanceOf(CanalMensagemWhatsapp.CanalIndisponivelException.class)
                .hasMessageContaining("desabilitado");

        verify(clienteEvolutionApi, never()).enviarTexto(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void deveFalharDeFormaRastreavelQuandoHabilitadoSemConfiguracaoCompleta() {
        propriedades.setHabilitado(true);
        propriedades.setUrlBase("http://localhost:8080");

        assertThatThrownBy(() -> canal.enviar(envio("11988887777")))
                .isInstanceOf(CanalMensagemWhatsapp.CanalIndisponivelException.class);
    }

    @Test
void deveFalharDeFormaRastreavelQuandoWhatsappAusente() {
        habilitarCanalCompleto();

        assertThatThrownBy(() -> canal.enviar(envio(null)))
                .isInstanceOf(CanalMensagemWhatsapp.TelefoneInvalidoException.class)
                .hasMessageContaining("não informado");

        verify(clienteEvolutionApi, never()).enviarTexto(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void deveFalharDeFormaRastreavelQuandoWhatsappInvalido() {
        habilitarCanalCompleto();

        assertThatThrownBy(() -> canal.enviar(envio("123")))
                .isInstanceOf(CanalMensagemWhatsapp.TelefoneInvalidoException.class)
                .hasMessageContaining("WhatsApp do membro não informado ou inválido");
    }

    @Test
    void devePropagarErroDoClienteParaPermitirRegistroDeFalha() {
        habilitarCanalCompleto();
        doThrow(new RuntimeException("Evolution API respondeu 401"))
                .when(clienteEvolutionApi).enviarTexto(
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());

        assertThatThrownBy(() -> canal.enviar(envio("11988887777")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("401");
    }
}