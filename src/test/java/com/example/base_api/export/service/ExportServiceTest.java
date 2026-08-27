package com.example.base_api.export.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.disponibilidade.repository.DisponibilidadeSemanalRepository;
import com.example.base_api.disponibilidade.repository.IndisponibilidadeRepository;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaServicoRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
import com.example.base_api.export.dto.ExportFile;
import com.example.base_api.export.dto.FormatoExportacao;
import com.example.base_api.jornada.repository.JornadaMembroRepository;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaHistoricoRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private ExportRenderer exportRenderer;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private PessoaHistoricoRepository pessoaHistoricoRepository;
    @Mock
    private JornadaMembroRepository jornadaMembroRepository;
    @Mock
    private MinisterioRepository ministerioRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private PessoaServicoRepository pessoaServicoRepository;
    @Mock
    private EscalaRepository escalaRepository;
    @Mock
    private EscalaServicoRepository escalaServicoRepository;
    @Mock
    private EscalaVoluntarioRepository escalaVoluntarioRepository;
    @Mock
    private DisponibilidadeSemanalRepository disponibilidadeSemanalRepository;
    @Mock
    private IndisponibilidadeRepository indisponibilidadeRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AutorizacaoService autorizacaoService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ExportService exportService;

    @Test
    void filialNaoExportaRedeInteira() {
        UUID filialId = UUID.randomUUID();
        Pessoa pessoaFilial = new Pessoa();
        pessoaFilial.setId(UUID.randomUUID());
        pessoaFilial.setNome("Filial");
        pessoaFilial.setIgrejaId(filialId);

        when(autorizacaoService.igrejaAtualId()).thenReturn(filialId);
        when(autorizacaoService.idsParaListagem(null)).thenReturn(List.of(filialId));
        when(pessoaRepository.findByIgrejaIdInOrderByNomeAsc(List.of(filialId)))
                .thenReturn(List.of(pessoaFilial));
        when(ministerioRepository.findByIgrejaIdInOrderByNomeAsc(List.of(filialId))).thenReturn(List.of());
        when(escalaRepository.findByIgrejaIdInOrderByCompetenciaDescTituloAsc(List.of(filialId)))
                .thenReturn(List.of());
        when(usuarioRepository.findByIgrejaIdIn(List.of(filialId))).thenReturn(List.of());
        when(exportRenderer.renderizar(any(), any()))
                .thenReturn(new ExportFile("pessoas.txt", "text/plain", new byte[0]));

        exportService.exportar("pessoas", FormatoExportacao.TXT);

        verify(pessoaRepository).findByIgrejaIdInOrderByNomeAsc(List.of(filialId));
        verify(pessoaRepository, never()).findAll();
        verify(ministerioRepository, never()).findAll();
        verify(escalaRepository, never()).findAll();
        verify(usuarioRepository, never()).findAll();
    }
}
