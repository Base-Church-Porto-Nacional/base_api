package com.example.base_api.servico.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.service.MinisterioService;
import com.example.base_api.servico.dto.ServicoCreateRequest;
import com.example.base_api.servico.dto.ServicoResponse;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.repository.ServicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private MinisterioService ministerioService;

    @InjectMocks
    private ServicoService servicoService;

    @Test
    void deveCriarServicoNoMinisterio() {
        UUID ministerioId = UUID.randomUUID();
        Ministerio ministerio = new Ministerio();
        ministerio.setId(ministerioId);
        ministerio.setNome("Louvor");

        when(ministerioService.buscarEntidade(ministerioId)).thenReturn(ministerio);
        when(servicoRepository.findByMinisterioIdAndNomeIgnoreCase(ministerioId, "Violão"))
                .thenReturn(Optional.empty());
        when(servicoRepository.save(any(Servico.class))).thenAnswer(invocation -> {
            Servico s = invocation.getArgument(0);
            s.setId(UUID.randomUUID());
            s.setCreatedAt(Instant.now());
            s.setUpdatedAt(Instant.now());
            return s;
        });

        ServicoResponse response = servicoService.criar(ministerioId, new ServicoCreateRequest("Violão", null));

        assertThat(response.nome()).isEqualTo("Violão");
        assertThat(response.ministerioId()).isEqualTo(ministerioId);
    }

    @Test
    void naoDeveCriarServicoDuplicadoNoMesmoMinisterio() {
        UUID ministerioId = UUID.randomUUID();
        when(ministerioService.buscarEntidade(ministerioId)).thenReturn(new Ministerio());
        when(servicoRepository.findByMinisterioIdAndNomeIgnoreCase(ministerioId, "Violão"))
                .thenReturn(Optional.of(new Servico()));

        assertThatThrownBy(() -> servicoService.criar(ministerioId, new ServicoCreateRequest("Violão", null)))
                .isInstanceOf(BusinessException.class);
    }
}
