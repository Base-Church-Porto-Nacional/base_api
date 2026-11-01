package com.example.base_api.culto.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.culto.entity.CategoriaCulto;
import com.example.base_api.culto.entity.Culto;
import com.example.base_api.culto.entity.DiaSemana;
import com.example.base_api.culto.entity.StatusCulto;
import com.example.base_api.culto.repository.CategoriaCultoRepository;
import com.example.base_api.culto.repository.CultoRepository;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.security.AutorizacaoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CultoAutomacaoServiceTest {

    @Mock
    private CategoriaCultoRepository categoriaRepository;
    @Mock
    private CultoRepository cultoRepository;
    @Mock
    private IgrejaRepository igrejaRepository;
    @Mock
    private AutorizacaoService autorizacaoService;

    @InjectMocks
    private CultoAutomacaoService automacaoService;

    private CategoriaCulto categoriaDomingo(UUID igrejaId) {
        CategoriaCulto categoria = new CategoriaCulto();
        categoria.setId(UUID.randomUUID());
        categoria.setIgrejaId(igrejaId);
        categoria.setTitulo("Domingo inesquecível");
        categoria.setDiaSemana(DiaSemana.DOMINGO);
        categoria.setHoraInicio(LocalTime.of(20, 0));
        categoria.setDuracaoMinutos(90);
        categoria.setAtivo(true);
        return categoria;
    }

    @Test
    void deveGerarCultosDoMesParaTodasAsOcorrencias() {
        UUID igrejaId = UUID.randomUUID();
        CategoriaCulto categoria = categoriaDomingo(igrejaId);

        when(autorizacaoService.idsParaListagem(null)).thenReturn(List.of(igrejaId));
        when(categoriaRepository.findByIgrejaIdInAndAtivoOrderByTituloAsc(any(), anyBoolean()))
                .thenReturn(List.of(categoria));
        when(cultoRepository.existsByCategoriaCultoIdAndData(any(), any())).thenReturn(false);
        when(cultoRepository.save(any(Culto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        YearMonth mes = YearMonth.of(2026, 9);
        long domingos = mes.atDay(1).datesUntil(mes.atEndOfMonth().plusDays(1))
                .filter(data -> data.getDayOfWeek() == DayOfWeek.SUNDAY)
                .count();

        String resumo = automacaoService.gerarMesManual("2026-09", null);

        assertThat(resumo).contains(domingos + " criados");
        verify(cultoRepository, org.mockito.Mockito.times((int) domingos)).save(any(Culto.class));
    }

    @Test
    void devePularCultosQueJaExistem() {
        UUID igrejaId = UUID.randomUUID();
        CategoriaCulto categoria = categoriaDomingo(igrejaId);

        when(autorizacaoService.idsParaListagem(null)).thenReturn(List.of(igrejaId));
        when(categoriaRepository.findByIgrejaIdInAndAtivoOrderByTituloAsc(any(), anyBoolean()))
                .thenReturn(List.of(categoria));
        when(cultoRepository.existsByCategoriaCultoIdAndData(any(), any())).thenReturn(true);

        String resumo = automacaoService.gerarMesManual("2026-09", null);

        assertThat(resumo).contains("0 criados, 4 já existentes");
        verify(cultoRepository, never()).save(any(Culto.class));
    }

    @Test
    void deveGerarParaCategoriaEspecifica() {
        UUID igrejaId = UUID.randomUUID();
        CategoriaCulto categoria = categoriaDomingo(igrejaId);

        when(categoriaRepository.findById(categoria.getId())).thenReturn(Optional.of(categoria));
        when(cultoRepository.existsByCategoriaCultoIdAndData(any(), any())).thenReturn(false);
        when(cultoRepository.save(any(Culto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        YearMonth mes = YearMonth.now().plusMonths(1);
        long domingos = mes.atDay(1).datesUntil(mes.atEndOfMonth().plusDays(1))
                .filter(data -> data.getDayOfWeek() == DayOfWeek.SUNDAY)
                .count();

        String resumo = automacaoService.gerarProximoMesDaCategoria(categoria.getId());

        assertThat(resumo).contains(domingos + " criados");
        verify(cultoRepository, org.mockito.Mockito.times((int) domingos)).save(any(Culto.class));
    }

    @Test
    void deveRejeitarCompetenciaInvalida() {
        UUID igrejaId = UUID.randomUUID();

        assertThatThrownBy(() -> automacaoService.gerarMesManual("09/2026", igrejaId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deveRejeitarCompetenciaComMesInexistenteComoErroDeNegocio() {
        UUID igrejaId = UUID.randomUUID();

        assertThatThrownBy(() -> automacaoService.gerarMesManual("2026-13", igrejaId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Competência inválida");
    }

    @Test
    void deveCriarCultoComDadosHerdadosDaCategoria() {
        UUID igrejaId = UUID.randomUUID();
        UUID pregadorPadraoId = UUID.randomUUID();
        CategoriaCulto categoria = categoriaDomingo(igrejaId);
        categoria.setPregadorPadraoId(pregadorPadraoId);

        when(autorizacaoService.idsParaListagem(null)).thenReturn(List.of(igrejaId));
        when(categoriaRepository.findByIgrejaIdInAndAtivoOrderByTituloAsc(any(), anyBoolean()))
                .thenReturn(List.of(categoria));
        when(cultoRepository.existsByCategoriaCultoIdAndData(any(), any())).thenReturn(false);
        when(cultoRepository.save(any(Culto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        automacaoService.gerarMesManual("2026-09", null);

        ArgumentCaptor<Culto> captor = ArgumentCaptor.forClass(Culto.class);
        verify(cultoRepository, atLeastOnce()).save(captor.capture());
        Culto criado = captor.getValue();
        assertThat(criado.getIgrejaId()).isEqualTo(igrejaId);
        assertThat(criado.getCategoriaCultoId()).isEqualTo(categoria.getId());
        assertThat(criado.getTitulo()).isEqualTo("Domingo inesquecível");
        assertThat(criado.getHoraInicio()).isEqualTo(LocalTime.of(20, 0));
        assertThat(criado.getHoraFim()).isEqualTo(LocalTime.of(21, 30));
        assertThat(criado.getPregadorId()).isEqualTo(pregadorPadraoId);
        assertThat(criado.getStatus()).isEqualTo(StatusCulto.AGENDADO);
        assertThat(criado.getAtivo()).isTrue();
        assertThat(criado.getCompetencia()).isEqualTo("2026-09");
    }

    @Test
    void deveGerarSomenteNosDiasDaSemanaDaCategoria() {
        UUID igrejaId = UUID.randomUUID();
        CategoriaCulto terca = categoriaTerca(igrejaId);

        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(categoriaRepository.findByIgrejaIdInAndAtivoOrderByTituloAsc(any(), anyBoolean()))
                .thenReturn(List.of(terca));
        when(cultoRepository.existsByCategoriaCultoIdAndData(any(), any())).thenReturn(false);
        when(cultoRepository.save(any(Culto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        automacaoService.gerarMesManual("2026-09", igrejaId);

        ArgumentCaptor<Culto> captor = ArgumentCaptor.forClass(Culto.class);
        verify(cultoRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .isNotEmpty()
                .allSatisfy(culto -> {
                    assertThat(culto.getData().getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
                    assertThat(culto.getHoraFim()).isEqualTo(LocalTime.of(20, 30));
                });
    }

    @Test
    void naoDeveGerarCultosQuandoNaoHaIgrejaVisivel() {
        UUID igrejaId = UUID.randomUUID();
        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of());

        String resumo = automacaoService.gerarMesManual("2026-09", igrejaId);

        assertThat(resumo).contains("Nenhuma igreja visível");
        verify(cultoRepository, never()).save(any(Culto.class));
    }

    @Test
    void naoDeveGerarCultosDeIgrejaSemPermissaoDePastorOuAdmin() {
        UUID igrejaId = UUID.randomUUID();
        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        doThrow(new BusinessException("Sem permissão para gerar cultos nesta igreja"))
                .when(autorizacaoService).assertPastorOuAdmin(igrejaId);

        assertThatThrownBy(() -> automacaoService.gerarMesManual("2026-09", igrejaId))
                .isInstanceOf(BusinessException.class);
        verify(cultoRepository, never()).save(any(Culto.class));
    }

    @Test
    void deveGerarProximoMesAgendadoParaTodasAsIgrejasAtivas() {
        UUID igrejaId = UUID.randomUUID();
        CategoriaCulto categoria = categoriaDomingo(igrejaId);

        Igreja igreja = new Igreja();
        igreja.setId(igrejaId);
        igreja.setNome("Matriz");
        igreja.setAtivo(true);

        when(igrejaRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(igreja));
        when(categoriaRepository.findByIgrejaIdInAndAtivoOrderByTituloAsc(any(), anyBoolean()))
                .thenReturn(List.of(categoria));
        when(cultoRepository.existsByCategoriaCultoIdAndData(any(), any())).thenReturn(false);
        when(cultoRepository.save(any(Culto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String competencia = YearMonth.now().plusMonths(1).toString();
        String resumo = automacaoService.gerarProximoMesAgendado();

        long domingos = YearMonth.parse(competencia).atDay(1)
                .datesUntil(YearMonth.parse(competencia).atEndOfMonth().plusDays(1))
                .filter(data -> data.getDayOfWeek() == DayOfWeek.SUNDAY)
                .count();
        assertThat(resumo).contains(competencia).contains(domingos + " criados");
        ArgumentCaptor<Culto> captor = ArgumentCaptor.forClass(Culto.class);
        verify(cultoRepository, times((int) domingos)).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(culto -> {
            assertThat(culto.getIgrejaId()).isEqualTo(igrejaId);
            assertThat(culto.getCompetencia()).isEqualTo(competencia);
        });
    }

    @Test
    void naoDeveGerarNadaQuandoNaoHaIgrejaAtiva() {
        when(igrejaRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of());

        String resumo = automacaoService.gerarProximoMesAgendado();

        assertThat(resumo).contains("Nenhuma igreja ativa");
        verify(cultoRepository, never()).save(any(Culto.class));
    }

    @Test
    void deveGerarParteDosCultosFaltandoSemDuplicarOsExistentes() {
        UUID igrejaId = UUID.randomUUID();
        CategoriaCulto categoria = categoriaDomingo(igrejaId);

        LocalDate primeiroDomingo = YearMonth.of(2026, 9)
                .atDay(1).datesUntil(YearMonth.of(2026, 9).atEndOfMonth().plusDays(1))
                .filter(data -> data.getDayOfWeek() == DayOfWeek.SUNDAY)
                .findFirst()
                .orElseThrow();

        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(categoriaRepository.findByIgrejaIdInAndAtivoOrderByTituloAsc(any(), anyBoolean()))
                .thenReturn(List.of(categoria));
        when(cultoRepository.existsByCategoriaCultoIdAndData(categoria.getId(), primeiroDomingo))
                .thenReturn(true);
        when(cultoRepository.save(any(Culto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String resumo = automacaoService.gerarMesManual("2026-09", igrejaId);

        assertThat(resumo).contains("3 criados, 1 já existentes");
        verify(cultoRepository, times(3)).save(any(Culto.class));
    }

    @Test
    void deveRejeitarGeracaoDeCategoriaInexistente() {
        assertThatThrownBy(() -> automacaoService.gerarMesDaCategoria(UUID.randomUUID(), "2026-09"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Categoria de culto não encontrada");
    }

    @Test
    void naoDeveGerarCultosDeCategoriaDeIgrejaForaDoAcesso() {
        UUID igrejaId = UUID.randomUUID();
        CategoriaCulto categoria = categoriaDomingo(igrejaId);
        when(categoriaRepository.findById(categoria.getId())).thenReturn(Optional.of(categoria));
        doThrow(new BusinessException("Sem permissão nesta igreja"))
                .when(autorizacaoService).assertPastorOuAdmin(igrejaId);

        assertThatThrownBy(() -> automacaoService.gerarMesDaCategoria(categoria.getId(), "2026-09"))
                .isInstanceOf(BusinessException.class);
        verify(cultoRepository, never()).save(any(Culto.class));
    }

    private CategoriaCulto categoriaTerca(UUID igrejaId) {
        CategoriaCulto categoria = new CategoriaCulto();
        categoria.setId(UUID.randomUUID());
        categoria.setIgrejaId(igrejaId);
        categoria.setTitulo("Terça de Libertação");
        categoria.setDiaSemana(DiaSemana.TERCA);
        categoria.setHoraInicio(LocalTime.of(19, 30));
        categoria.setDuracaoMinutos(60);
        categoria.setAtivo(true);
        return categoria;
    }
}