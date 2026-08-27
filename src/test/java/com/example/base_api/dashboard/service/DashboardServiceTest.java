package com.example.base_api.dashboard.service;

import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import com.example.base_api.dashboard.dto.DashboardResponse;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.repository.PessoaPausaRepository;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.servico.repository.ServicoRepository;
import com.example.base_api.usuario.repository.UsuarioRepository;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private MinisterioRepository ministerioRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private PessoaServicoRepository pessoaServicoRepository;
    @Mock
    private EscalaRepository escalaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CronogramaEventoRepository cronogramaEventoRepository;
    @Mock
    private PessoaPausaRepository pessoaPausaRepository;
    @Mock
    private AutorizacaoService autorizacaoService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void resumoUsaIgrejasVisiveisNuncaCountGlobal() {
        UUID igrejaId = UUID.randomUUID();
        List<UUID> ids = List.of(igrejaId);

        when(autorizacaoService.igrejaAtualId()).thenReturn(igrejaId);
        when(autorizacaoService.idsParaListagem(null)).thenReturn(ids);
        when(pessoaRepository.findByIgrejaIdInOrderByNomeAsc(ids)).thenReturn(List.of());
        when(ministerioRepository.findByIgrejaIdInOrderByNomeAsc(ids)).thenReturn(List.of());
        when(pessoaRepository.countByIgrejaIdIn(ids)).thenReturn(3L);
        when(pessoaRepository.countByIgrejaIdInAndAtivoTrue(ids)).thenReturn(2L);
        when(ministerioRepository.countByIgrejaIdIn(ids)).thenReturn(1L);
        when(escalaRepository.countByIgrejaIdIn(ids)).thenReturn(4L);
        when(usuarioRepository.countByIgrejaIdIn(ids)).thenReturn(5L);
        when(usuarioRepository.findAtivosByPapelAndIgrejaIdIn("LIDER", ids)).thenReturn(List.of());
        when(cronogramaEventoRepository.countByIgrejaIdIn(ids)).thenReturn(6L);

        DashboardResponse resumo = dashboardService.obterResumo();

        assertThat(resumo.totalPessoas()).isEqualTo(3);
        assertThat(resumo.totalPessoasAtivas()).isEqualTo(2);
        verify(pessoaRepository, never()).count();
        verify(pessoaRepository).countByIgrejaIdIn(ids);
        verify(pessoaServicoRepository, never()).findAll();
        verify(pessoaPausaRepository, never()).findAll();
    }
}
