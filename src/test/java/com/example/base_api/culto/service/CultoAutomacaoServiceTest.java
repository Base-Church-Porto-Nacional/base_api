package com.example.base_api.culto.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.culto.entity.CategoriaCulto;
import com.example.base_api.culto.entity.Culto;
import com.example.base_api.culto.entity.DiaSemana;
import com.example.base_api.culto.repository.CategoriaCultoRepository;
import com.example.base_api.culto.repository.CultoRepository;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.security.AutorizacaoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
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
}