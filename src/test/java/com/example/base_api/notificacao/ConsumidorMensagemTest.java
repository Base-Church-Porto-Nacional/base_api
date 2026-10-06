package com.example.base_api.notificacao;

import com.example.base_api.aniversario.service.AniversarioEnvioStatusService;
import com.example.base_api.notificacao.evento.EventoMensagem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConsumidorMensagemTest {

    @Mock
    private CanalMensagem canalMensagem;
    @Mock
    private AniversarioEnvioStatusService statusService;

    private ConsumidorMensagem consumidor;

    private EventoMensagem evento;

    @BeforeEach
    void setUp() {
        consumidor = new ConsumidorMensagem(canalMensagem, statusService);
        evento = EventoMensagem.whatsapp(
                UUID.randomUUID(), "Maria", "5511999994444", "maria@email.com",
                "Feliz aniversário!", UUID.randomUUID());
    }

    @Test
    void deveEntregarNoCanalERegistrarEnviado() {
        consumidor.consumir(evento);

        ArgumentCaptor<CanalMensagem.EnvioMensagem> captor =
                ArgumentCaptor.forClass(CanalMensagem.EnvioMensagem.class);
        verify(canalMensagem).enviar(captor.capture());
        assertThat(captor.getValue().whatsapp()).isEqualTo("5511999994444");
        assertThat(captor.getValue().mensagem()).isEqualTo("Feliz aniversário!");
        verify(statusService).registrarEnviado(evento.envioId());
        verify(statusService, never()).registrarFalha(any(), any());
    }

    @Test
    void deveRegistrarFalhaERelancarParaQueORetryAja() {
        RuntimeException erro = new IllegalStateException("evolution api fora do ar");
        doThrow(erro).when(canalMensagem).enviar(any());

        assertThatThrownBy(() -> consumidor.consumir(evento))
                .isSameAs(erro);

        verify(statusService).registrarFalha(evento.envioId(), erro);
        verify(statusService, never()).registrarEnviado(any());
    }
}