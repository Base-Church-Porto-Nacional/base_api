package com.example.base_api.disponibilidade.service;

import com.example.base_api.disponibilidade.dto.DisponibilidadeSemanalRequest;
import com.example.base_api.common.entity.DiaSemana;
import com.example.base_api.disponibilidade.repository.DisponibilidadeSemanalRepository;
import com.example.base_api.disponibilidade.repository.IndisponibilidadeRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.security.AutorizacaoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadeServiceTest {

    @Mock
    private DisponibilidadeSemanalRepository disponibilidadeSemanalRepository;
    @Mock
    private IndisponibilidadeRepository indisponibilidadeRepository;
    @Mock
    private PessoaService pessoaService;
    @Mock
    private AutorizacaoService autorizacaoService;

    @InjectMocks
    private DisponibilidadeService disponibilidadeService;

    @Test
    void voluntarioNaoAlteraDisponibilidadeDeOutroPessoaId() {
        UUID outroPessoaId = UUID.randomUUID();
        Pessoa outra = new Pessoa();
        outra.setId(outroPessoaId);

        when(pessoaService.buscarEntidade(outroPessoaId)).thenReturn(outra);
        doThrow(new AccessDeniedException("Só é possível gerir a própria disponibilidade"))
                .when(autorizacaoService).assertPodeAcessarDisponibilidade(outra);

        DisponibilidadeSemanalRequest request = new DisponibilidadeSemanalRequest(
                outroPessoaId,
                DiaSemana.DOMINGO,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                true,
                null
        );

        assertThatThrownBy(() -> disponibilidadeService.criarSemanal(request))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> disponibilidadeService.listarSemanal(outroPessoaId))
                .isInstanceOf(AccessDeniedException.class);
    }
}
