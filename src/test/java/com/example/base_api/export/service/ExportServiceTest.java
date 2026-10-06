package com.example.base_api.export.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.culto.entity.CategoriaCulto;
import com.example.base_api.culto.entity.Culto;
import com.example.base_api.culto.entity.CultoRelatorio;
import com.example.base_api.culto.entity.StatusCulto;
import com.example.base_api.culto.repository.CategoriaCultoRepository;
import com.example.base_api.culto.repository.CultoRelatorioRepository;
import com.example.base_api.culto.repository.CultoRepository;
import com.example.base_api.disponibilidade.repository.DisponibilidadeSemanalRepository;
import com.example.base_api.disponibilidade.repository.IndisponibilidadeRepository;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaServicoRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
import com.example.base_api.export.dto.ExportDocument;
import com.example.base_api.export.dto.ExportFile;
import com.example.base_api.export.dto.ExportSheet;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private CultoRepository cultoRepository;
    @Mock
    private CultoRelatorioRepository cultoRelatorioRepository;
    @Mock
    private CategoriaCultoRepository categoriaCultoRepository;
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

    @Test
    void deveExportarCultosComContagem() {
        UUID igrejaId = UUID.randomUUID();
        CategoriaCulto categoria = new CategoriaCulto();
        categoria.setId(UUID.randomUUID());
        categoria.setTitulo("Culto de Domingo");
        Culto realizado = culto(igrejaId, categoria.getId(), LocalDate.of(2026, 9, 6), StatusCulto.REALIZADO);
        Culto agendado = culto(igrejaId, categoria.getId(), LocalDate.of(2026, 9, 13), StatusCulto.AGENDADO);
        CultoRelatorio relatorio = CultoRelatorio.vazio(realizado.getId());
        relatorio.setAdultos(90);
        relatorio.setCriancas(15);
        relatorio.setCarros(30);
        relatorio.setMotos(5);

        when(autorizacaoService.igrejaAtualId()).thenReturn(igrejaId);
        when(autorizacaoService.idsParaListagem(null)).thenReturn(List.of(igrejaId));
        when(cultoRepository.filtrar(List.of(igrejaId), null, null, null, null, null, null))
                .thenReturn(List.of(realizado, agendado));
        when(cultoRelatorioRepository.findByCultoIdIn(List.of(realizado.getId(), agendado.getId())))
                .thenReturn(List.of(relatorio));
        when(categoriaCultoRepository.findAllById(List.of(categoria.getId()))).thenReturn(List.of(categoria));
        when(exportRenderer.renderizar(any(), eq(FormatoExportacao.XLSX)))
                .thenReturn(new ExportFile("cultos.xlsx", "application/xlsx", new byte[0]));

        exportService.exportar("cultos", FormatoExportacao.XLSX);

        ArgumentCaptor<ExportDocument> captor = ArgumentCaptor.forClass(ExportDocument.class);
        verify(exportRenderer).renderizar(captor.capture(), eq(FormatoExportacao.XLSX));
        ExportSheet sheet = captor.getValue().getSheets().getFirst();
        assertThat(sheet.getTitulo()).isEqualTo("Cultos");
        assertThat(sheet.getLinhas()).hasSize(2);
        List<String> linhaRealizado = sheet.getLinhas().getFirst();
        assertThat(linhaRealizado.get(3)).isEqualTo("Culto de Domingo");
        assertThat(linhaRealizado.get(10)).isEqualTo("105");
        assertThat(linhaRealizado.get(17)).isEqualTo("35");
        assertThat(sheet.getLinhas().get(1).get(10)).isEmpty();
        verify(auditService).registrar("EXPORTAR", "cultos", "formato=XLSX");
    }

    private Culto culto(UUID igrejaId, UUID categoriaId, LocalDate data, StatusCulto status) {
        Culto culto = new Culto();
        culto.setId(UUID.randomUUID());
        culto.setIgrejaId(igrejaId);
        culto.setCategoriaCultoId(categoriaId);
        culto.setTitulo("Culto");
        culto.setData(data);
        culto.setHoraInicio(LocalTime.of(19, 0));
        culto.setCompetencia(data.toString().substring(0, 7));
        culto.setStatus(status);
        return culto;
    }
}
