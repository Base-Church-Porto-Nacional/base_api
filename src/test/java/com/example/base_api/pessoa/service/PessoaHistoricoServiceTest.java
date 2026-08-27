package com.example.base_api.pessoa.service;

import com.example.base_api.pessoa.dto.PessoaHistoricoCreateRequest;
import com.example.base_api.pessoa.dto.PessoaHistoricoResponse;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.entity.PessoaHistorico;
import com.example.base_api.pessoa.repository.PessoaHistoricoRepository;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PessoaHistoricoServiceTest {

    @Mock
    private PessoaHistoricoRepository historicoRepository;

    @Mock
    private PessoaRepository pessoaRepository;

    @Mock
    private AutorizacaoService autorizacaoService;

    @InjectMocks
    private PessoaHistoricoService pessoaHistoricoService;

    @Test
    void deveCriarRegistroDeHistorico() {
        UUID pessoaId = UUID.randomUUID();
        Pessoa pessoa = new Pessoa();
        pessoa.setId(pessoaId);
        pessoa.setIgrejaId(UUID.randomUUID());
        when(pessoaRepository.findById(pessoaId)).thenReturn(Optional.of(pessoa));
        when(historicoRepository.save(any(PessoaHistorico.class))).thenAnswer(invocation -> {
            PessoaHistorico h = invocation.getArgument(0);
            h.setId(UUID.randomUUID());
            h.setCreatedAt(Instant.now());
            h.setUpdatedAt(Instant.now());
            return h;
        });

        PessoaHistoricoResponse response = pessoaHistoricoService.criar(
                pessoaId,
                new PessoaHistoricoCreateRequest(
                        "Voluntário pediu para ficar no banco por 15 dias devido a problema na coluna",
                        "Carla",
                        "Pastora",
                        null,
                        LocalDate.of(2026, 8, 20)
                )
        );

        assertThat(response.descricao()).contains("ficar no banco");
        assertThat(response.autorNome()).isEqualTo("Carla");
        assertThat(response.autorCargo()).isEqualTo("Pastora");
        assertThat(response.dataRegistro()).isEqualTo(LocalDate.of(2026, 8, 20));
    }

    @Test
    void membroNaoEditaHistoricoAlheio() {
        UUID pessoaId = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        Pessoa pessoa = new Pessoa();
        pessoa.setId(pessoaId);
        pessoa.setIgrejaId(igrejaId);
        when(pessoaRepository.findById(pessoaId)).thenReturn(Optional.of(pessoa));
        doThrow(new AccessDeniedException("Sem permissão"))
                .when(autorizacaoService).assertPodeMutarPessoas(igrejaId);

        assertThatThrownBy(() -> pessoaHistoricoService.criar(
                pessoaId,
                new PessoaHistoricoCreateRequest("nota", "Autor", null, null, LocalDate.now())
        )).isInstanceOf(AccessDeniedException.class);
        verify(historicoRepository, never()).save(any());
    }
}
