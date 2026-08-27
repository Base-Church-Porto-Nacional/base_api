package com.example.base_api.jornada.service;

import com.example.base_api.jornada.dto.JornadaUpdateRequest;
import com.example.base_api.jornada.repository.JornadaEtapaCatalogoRepository;
import com.example.base_api.jornada.repository.JornadaMembroRepository;
import com.example.base_api.jornada.repository.PessoaJornadaEtapaRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JornadaServiceTest {

    @Mock
    private JornadaMembroRepository jornadaMembroRepository;
    @Mock
    private JornadaEtapaCatalogoRepository etapaCatalogoRepository;
    @Mock
    private PessoaJornadaEtapaRepository pessoaJornadaEtapaRepository;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private AutorizacaoService autorizacaoService;

    @InjectMocks
    private JornadaService jornadaService;

    @Test
    void naoEditaJornadaAlheia() {
        UUID pessoaId = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        Pessoa pessoa = new Pessoa();
        pessoa.setId(pessoaId);
        pessoa.setIgrejaId(igrejaId);

        when(pessoaRepository.findById(pessoaId)).thenReturn(Optional.of(pessoa));
        doThrow(new AccessDeniedException("Sem permissão"))
                .when(autorizacaoService).assertPodeMutarPessoas(igrejaId);

        JornadaUpdateRequest request = new JornadaUpdateRequest(
                true, null, true, null, true, null, true, null
        );

        assertThatThrownBy(() -> jornadaService.atualizar(pessoaId, request))
                .isInstanceOf(AccessDeniedException.class);
        verify(jornadaMembroRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
