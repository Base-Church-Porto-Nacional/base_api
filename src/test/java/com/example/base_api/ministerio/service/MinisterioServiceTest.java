package com.example.base_api.ministerio.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.ministerio.dto.MinisterioCreateRequest;
import com.example.base_api.ministerio.dto.MinisterioResponse;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.entity.MinisterioTipo;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.repository.ServicoRepository;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinisterioServiceTest {

    @Mock
    private MinisterioRepository ministerioRepository;
    @Mock
    private AutorizacaoService autorizacaoService;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private PessoaServicoRepository pessoaServicoRepository;

    @InjectMocks
    private MinisterioService ministerioService;

    @Test
    void deveCriarMinisterioComServicoCoordenador() {
        UUID igrejaId = UUID.randomUUID();
        when(ministerioRepository.findByIgrejaIdAndNomeIgnoreCase(igrejaId, "Louvor")).thenReturn(Optional.empty());
        when(ministerioRepository.save(any(Ministerio.class))).thenAnswer(invocation -> {
            Ministerio m = invocation.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(Instant.now());
            m.setUpdatedAt(Instant.now());
            return m;
        });
        when(servicoRepository.findByMinisterioIdAndNomeIgnoreCase(any(), eq("Coordenador")))
                .thenReturn(Optional.empty());
        when(servicoRepository.save(any(Servico.class))).thenAnswer(invocation -> {
            Servico s = invocation.getArgument(0);
            s.setId(UUID.randomUUID());
            s.setCreatedAt(Instant.now());
            s.setUpdatedAt(Instant.now());
            return s;
        });
        when(servicoRepository.findByMinisterioIdOrderByNomeAsc(any())).thenReturn(List.of());

        MinisterioResponse response = ministerioService.criar(
                new MinisterioCreateRequest("Louvor", "Ministério de louvor", true, igrejaId, MinisterioTipo.MUSICA, null, null)
        );

        assertThat(response.nome()).isEqualTo("Louvor");
        assertThat(response.ativo()).isTrue();
        assertThat(response.tipo()).isEqualTo(MinisterioTipo.MUSICA);
        assertThat(response.igrejaId()).isEqualTo(igrejaId);
    }

    @Test
    void naoDeveCriarMinisterioDuplicadoNaMesmaIgreja() {
        UUID igrejaId = UUID.randomUUID();
        Ministerio existente = new Ministerio();
        existente.setId(UUID.randomUUID());
        existente.setNome("Louvor");
        when(ministerioRepository.findByIgrejaIdAndNomeIgnoreCase(igrejaId, "Louvor")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> ministerioService.criar(
                new MinisterioCreateRequest("Louvor", null, true, igrejaId, MinisterioTipo.GERAL, null, null)
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void deveLancarQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(ministerioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ministerioService.buscarPorId(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
