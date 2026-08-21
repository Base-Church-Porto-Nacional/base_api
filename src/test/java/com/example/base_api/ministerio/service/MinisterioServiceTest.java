package com.example.base_api.ministerio.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.ministerio.dto.MinisterioCreateRequest;
import com.example.base_api.ministerio.dto.MinisterioResponse;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.repository.MinisterioRepository;
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
class MinisterioServiceTest {

    @Mock
    private MinisterioRepository ministerioRepository;

    @InjectMocks
    private MinisterioService ministerioService;

    @Test
    void deveCriarMinisterio() {
        when(ministerioRepository.findByNomeIgnoreCase("Louvor")).thenReturn(Optional.empty());
        when(ministerioRepository.save(any(Ministerio.class))).thenAnswer(invocation -> {
            Ministerio m = invocation.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(Instant.now());
            m.setUpdatedAt(Instant.now());
            return m;
        });

        MinisterioResponse response = ministerioService.criar(
                new MinisterioCreateRequest("Louvor", "Ministério de louvor")
        );

        assertThat(response.nome()).isEqualTo("Louvor");
        assertThat(response.ativo()).isTrue();
    }

    @Test
    void naoDeveCriarMinisterioDuplicado() {
        Ministerio existente = new Ministerio();
        existente.setId(UUID.randomUUID());
        existente.setNome("Louvor");
        when(ministerioRepository.findByNomeIgnoreCase("Louvor")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> ministerioService.criar(new MinisterioCreateRequest("Louvor", null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deveLancarQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(ministerioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ministerioService.buscarPorId(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
