package com.example.base_api.escala.service;

import com.example.base_api.cronograma.entity.CronogramaEvento;
import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import com.example.base_api.escala.dto.GeracaoEscalaResponse;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.StatusEscala;
import com.example.base_api.escala.entity.TipoEscala;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaServicoRepository;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.repository.ServicoRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscalaAutomacaoServiceTest {

    @Mock
    private GeracaoEscalaService geracaoEscalaService;
    @Mock
    private EscalaRepository escalaRepository;
    @Mock
    private EscalaServicoRepository escalaServicoRepository;
    @Mock
    private IgrejaRepository igrejaRepository;
    @Mock
    private MinisterioRepository ministerioRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private CronogramaEventoRepository cronogramaEventoRepository;
    @Mock
    private PessoaServicoRepository pessoaServicoRepository;
    @Mock
    private AutorizacaoService autorizacaoService;

    @InjectMocks
    private EscalaAutomacaoService escalaAutomacaoService;

    private UUID igrejaId;
    private UUID servicoId;
    private UUID escalaId;

    @BeforeEach
    void setUp() {
        igrejaId = UUID.randomUUID();
        servicoId = UUID.randomUUID();
        escalaId = UUID.randomUUID();
    }

    @Test
    void deveCriarEscalaDeCultoComStatusSugerida() {
        Igreja igreja = igreja();
        when(igrejaRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(igreja));
        prepararEventosNoMes();
        when(ministerioRepository.findByIgrejaIdOrderByNomeAsc(igrejaId)).thenReturn(List.of());
        when(escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipo(any(), any(), any()))
                .thenReturn(Optional.empty());
        Servico servico = servico();
        when(servicoRepository.findByMinisterioIdInAndAtivoTrueOrderByNomeAsc(anyList()))
                .thenReturn(List.of(servico));
        when(pessoaServicoRepository.findByServicoIdAndAtivoTrue(servicoId)).thenReturn(List.of(pessoaServico()));
        when(escalaRepository.save(any(Escala.class))).thenAnswer(inv -> {
            Escala e = inv.getArgument(0);
            e.setId(escalaId);
            return e;
        });
        when(geracaoEscalaService.gerar(any(Escala.class)))
                .thenReturn(new GeracaoEscalaResponse(escalaId, "COMPLETA", "ok", List.of()));

        String resumo = escalaAutomacaoService.gerarProximoMes();

        assertThat(resumo).contains("1 criadas");
        ArgumentCaptor<Escala> captor = ArgumentCaptor.forClass(Escala.class);
        verify(escalaRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusEscala.SUGERIDA);
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoEscala.CULTO);
        assertThat(captor.getValue().getCompetencia()).isEqualTo(YearMonth.now().plusMonths(1).toString());
        verify(escalaServicoRepository).save(any());
    }

    @Test
    void deveAtualizarEscalaEmRascunhoSemCriarNova() {
        Igreja igreja = igreja();
        when(igrejaRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(igreja));
        prepararEventosNoMes();
        when(ministerioRepository.findByIgrejaIdOrderByNomeAsc(igrejaId)).thenReturn(List.of());
        Escala existente = escala();
        existente.setStatus(StatusEscala.SUGERIDA);
        when(escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipo(any(), any(), any()))
                .thenReturn(Optional.of(existente));
        when(geracaoEscalaService.gerar(existente))
                .thenReturn(new GeracaoEscalaResponse(escalaId, "COMPLETA", "ok", List.of()));

        String resumo = escalaAutomacaoService.gerarProximoMes();

        assertThat(resumo).contains("1 atualizadas");
        assertThat(resumo).contains("0 criadas");
        verify(escalaRepository, never()).save(any(Escala.class));
    }

    @Test
    void naoDeveRegenerarEscalaLancada() {
        Igreja igreja = igreja();
        when(igrejaRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(igreja));
        prepararEventosNoMes();
        when(ministerioRepository.findByIgrejaIdOrderByNomeAsc(igrejaId)).thenReturn(List.of());
        Escala existente = escala();
        existente.setStatus(StatusEscala.EM_ANDAMENTO);
        when(escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipo(any(), any(), any()))
                .thenReturn(Optional.of(existente));

        String resumo = escalaAutomacaoService.gerarProximoMes();

        assertThat(resumo).contains("0 criadas, 0 atualizadas");
        verify(geracaoEscalaService, never()).gerar(any(Escala.class));
    }

    @Test
    void naoDeveCriarEscalaSemEventosNoProximoMes() {
        Igreja igreja = igreja();
        when(igrejaRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(igreja));
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), any(), any())).thenReturn(List.of());

        String resumo = escalaAutomacaoService.gerarProximoMes();

        assertThat(resumo).contains("0 criadas, 0 atualizadas");
        verify(escalaRepository, never()).save(any(Escala.class));
        verify(geracaoEscalaService, never()).gerar(any(Escala.class));
    }

    @Test
    void naoDeveCriarEscalaQuandoNaoHouverVoluntarioNoServico() {
        Igreja igreja = igreja();
        when(igrejaRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(igreja));
        prepararEventosNoMes();
        when(ministerioRepository.findByIgrejaIdOrderByNomeAsc(igrejaId)).thenReturn(List.of());
        when(escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipo(any(), any(), any()))
                .thenReturn(Optional.empty());
        Servico servico = servico();
        when(servicoRepository.findByMinisterioIdInAndAtivoTrueOrderByNomeAsc(anyList()))
                .thenReturn(List.of(servico));
        when(pessoaServicoRepository.findByServicoIdAndAtivoTrue(servicoId)).thenReturn(List.of());

        String resumo = escalaAutomacaoService.gerarProximoMes();

        assertThat(resumo).contains("0 criadas, 0 atualizadas");
        verify(escalaRepository, never()).save(any(Escala.class));
    }

    private void prepararEventosNoMes() {
        String competencia = YearMonth.now().plusMonths(1).toString();
        LocalDate dia = YearMonth.now().plusMonths(1).atDay(1);
        CronogramaEvento evento = new CronogramaEvento();
        evento.setId(UUID.randomUUID());
        evento.setTitulo("Culto");
        evento.setDataEvento(dia);
        evento.setHoraInicio(LocalTime.of(18, 0));
        evento.setHoraFim(LocalTime.of(20, 0));
        evento.setIgrejaId(igrejaId);
        evento.setAtivo(true);
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), any(), any())).thenReturn(List.of(evento));
    }

    private Igreja igreja() {
        Igreja igreja = new Igreja();
        igreja.setId(igrejaId);
        igreja.setNome("Igreja Matriz");
        igreja.setPastorPessoaId(UUID.randomUUID());
        igreja.setAtivo(true);
        return igreja;
    }

    private Servico servico() {
        Servico servico = new Servico();
        servico.setId(servicoId);
        servico.setNome("Violão");
        servico.setFiccao(false);
        servico.setAtivo(true);
        return servico;
    }

    private PessoaServico pessoaServico() {
        PessoaServico v = new PessoaServico();
        v.setPessoaId(UUID.randomUUID());
        v.setServicoId(servicoId);
        v.setAtivo(true);
        return v;
    }

    private Escala escala() {
        Escala escala = new Escala();
        escala.setId(escalaId);
        escala.setTitulo("Escala de culto");
        escala.setCompetencia(YearMonth.now().plusMonths(1).toString());
        escala.setTipo(TipoEscala.CULTO);
        escala.setIgrejaId(igrejaId);
        escala.setAtivo(true);
        return escala;
    }
}