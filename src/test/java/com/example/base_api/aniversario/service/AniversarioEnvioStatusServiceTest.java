package com.example.base_api.aniversario.service;

import com.example.base_api.aniversario.entity.AniversarioEnvio;
import com.example.base_api.aniversario.entity.StatusEnvioAniversario;
import com.example.base_api.aniversario.repository.AniversarioEnvioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AniversarioEnvioStatusServiceTest {

    @Mock
    private AniversarioEnvioRepository envioRepository;

    private AniversarioEnvioStatusService statusService;

    private AniversarioEnvio envio;

    @BeforeEach
    void setUp() {
        statusService = new AniversarioEnvioStatusService(envioRepository);
        envio = new AniversarioEnvio();
        envio.setId(UUID.randomUUID());
        envio.setStatus(StatusEnvioAniversario.PENDENTE);
    }

    private void mockEnvioExistente() {
        when(envioRepository.findById(envio.getId())).thenReturn(Optional.of(envio));
    }

    @Test
    void deveMarcarEnviadoAPartirDePendente() {
        mockEnvioExistente();
        statusService.registrarEnviado(envio.getId());

        ArgumentCaptor<AniversarioEnvio> captor = ArgumentCaptor.forClass(AniversarioEnvio.class);
        verify(envioRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusEnvioAniversario.ENVIADO);
        assertThat(captor.getValue().getErro()).isNull();
    }

    @Test
    void deveMarcarFalhaComAMensagemDaCausa() {
        mockEnvioExistente();
        statusService.registrarFalha(envio.getId(), new IllegalStateException("evolution fora do ar"));

        ArgumentCaptor<AniversarioEnvio> captor = ArgumentCaptor.forClass(AniversarioEnvio.class);
        verify(envioRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusEnvioAniversario.FALHA);
        assertThat(captor.getValue().getErro()).isEqualTo("evolution fora do ar");
    }

    @Test
    void deveUsarClasseDaCausaQuandoNaoHaMensagem() {
        mockEnvioExistente();
        statusService.registrarFalha(envio.getId(), new IllegalStateException());

        ArgumentCaptor<AniversarioEnvio> captor = ArgumentCaptor.forClass(AniversarioEnvio.class);
        verify(envioRepository).save(captor.capture());
        assertThat(captor.getValue().getErro()).isEqualTo("IllegalStateException");
    }

    @Test
    void naoDeveSobrescreverStatusJaResolvido() {
        mockEnvioExistente();
        envio.setStatus(StatusEnvioAniversario.ENVIADO);

        statusService.registrarFalha(envio.getId(), new IllegalStateException("chegou atrasado"));

        verify(envioRepository, never()).save(any(AniversarioEnvio.class));
    }

    @Test
    void naoDeveFalharQuandoRegistroNaoExiste() {
        when(envioRepository.findById(any())).thenReturn(Optional.empty());

        statusService.registrarEnviado(UUID.randomUUID());

        verify(envioRepository, never()).save(any(AniversarioEnvio.class));
    }
}