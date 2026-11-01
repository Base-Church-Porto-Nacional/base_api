package com.example.base_api.escala.service;

import com.example.base_api.cronograma.entity.CronogramaEvento;
import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import com.example.base_api.escala.dto.GeracaoEscalaResponse;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.EscalaServico;
import com.example.base_api.escala.entity.StatusEscala;
import com.example.base_api.escala.entity.TipoEscala;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaServicoRepository;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.repository.ServicoRepository;
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    private EscalaAutomacaoService escalaAutomacaoService;

    /** Voluntários ativos por serviço, devolvidos pela consulta em lote de pessoaServicoRepository. */
    private final Map<UUID, List<PessoaServico>> voluntariosPorServico = new HashMap<>();

    private UUID igrejaId;
    private UUID servicoId;
    private UUID escalaId;

    @BeforeEach
    void setUp() {
        voluntariosPorServico.clear();
        lenient().when(pessoaServicoRepository.findByServicoIdInAndAtivoTrue(anyList())).thenAnswer(invocation -> {
            List<UUID> ids = invocation.getArgument(0);
            return ids.stream().flatMap(id -> voluntariosPorServico.getOrDefault(id, List.of()).stream()).toList();
        });
        escalaAutomacaoService = new EscalaAutomacaoService(geracaoEscalaService, escalaRepository, igrejaRepository, ministerioRepository, servicoRepository, cronogramaEventoRepository, autorizacaoService, new NecessidadeEscalaService(pessoaServicoRepository, escalaServicoRepository));
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
        voluntariosAtivos(servicoId, List.of(pessoaServico()));
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
        voluntariosAtivos(servicoId, List.of());

        String resumo = escalaAutomacaoService.gerarProximoMes();

        assertThat(resumo).contains("0 criadas, 0 atualizadas");
        verify(escalaRepository, never()).save(any(Escala.class));
    }

    @Test
    void deveCriarUmaEscalaParaCadaMinisterio() {
        Ministerio primeiro = ministerio();
        Ministerio segundo = ministerio();
        Servico servicoPrimeiro = servico();
        Servico servicoSegundo = servico();

        when(igrejaRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(igreja()));
        prepararEventosNoMes();
        when(ministerioRepository.findByIgrejaIdOrderByNomeAsc(igrejaId))
                .thenReturn(List.of(primeiro, segundo));
        when(escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipo(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(servicoRepository.findByMinisterioIdInAndAtivoTrueOrderByNomeAsc(anyList())).thenReturn(List.of());
        when(escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipoAndMinisterioId(
                any(), any(), any(), eq(primeiro.getId()))).thenReturn(Optional.empty());
        when(escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipoAndMinisterioId(
                any(), any(), any(), eq(segundo.getId()))).thenReturn(Optional.empty());
        when(servicoRepository.findByMinisterioIdOrderByNomeAsc(primeiro.getId()))
                .thenReturn(List.of(servicoPrimeiro));
        when(servicoRepository.findByMinisterioIdOrderByNomeAsc(segundo.getId()))
                .thenReturn(List.of(servicoSegundo));
        voluntariosAtivos(servicoPrimeiro.getId(), List.of(pessoaServico(servicoPrimeiro.getId())));
        voluntariosAtivos(servicoSegundo.getId(), List.of(pessoaServico(servicoSegundo.getId())));
        when(escalaRepository.save(any(Escala.class))).thenAnswer(inv -> {
            Escala e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        String resumo = escalaAutomacaoService.gerarProximoMes();

        assertThat(resumo).contains("2 criadas");
        ArgumentCaptor<Escala> captor = ArgumentCaptor.forClass(Escala.class);
        verify(escalaRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Escala::getTipo)
                .containsOnly(TipoEscala.MINISTERIO);
        assertThat(captor.getAllValues())
                .extracting(Escala::getMinisterioId)
                .containsExactlyInAnyOrder(primeiro.getId(), segundo.getId());
        assertThat(captor.getAllValues())
                .extracting(Escala::getStatus)
                .containsOnly(StatusEscala.SUGERIDA);
    }

    @Test
    void naoDeveReaproveitarEscalaDeOutroMinisterio() {
        Ministerio primeiro = ministerio();
        Ministerio segundo = ministerio();
        Escala existenteDoPrimeiro = escala();
        existenteDoPrimeiro.setTipo(TipoEscala.MINISTERIO);
        existenteDoPrimeiro.setMinisterioId(primeiro.getId());
        existenteDoPrimeiro.setStatus(StatusEscala.SUGERIDA);
        Servico servicoSegundo = servico();

        when(igrejaRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(igreja()));
        prepararEventosNoMes();
        when(ministerioRepository.findByIgrejaIdOrderByNomeAsc(igrejaId))
                .thenReturn(List.of(primeiro, segundo));
        when(escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipo(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(servicoRepository.findByMinisterioIdInAndAtivoTrueOrderByNomeAsc(anyList())).thenReturn(List.of());
        when(escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipoAndMinisterioId(
                any(), any(), any(), eq(primeiro.getId()))).thenReturn(Optional.of(existenteDoPrimeiro));
        when(escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipoAndMinisterioId(
                any(), any(), any(), eq(segundo.getId()))).thenReturn(Optional.empty());
        when(servicoRepository.findByMinisterioIdOrderByNomeAsc(segundo.getId()))
                .thenReturn(List.of(servicoSegundo));
        voluntariosAtivos(servicoSegundo.getId(), List.of(pessoaServico(servicoSegundo.getId())));
        when(escalaRepository.save(any(Escala.class))).thenAnswer(inv -> {
            Escala e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        String resumo = escalaAutomacaoService.gerarProximoMes();

        assertThat(resumo).contains("1 criadas, 1 atualizadas");
        ArgumentCaptor<Escala> captor = ArgumentCaptor.forClass(Escala.class);
        verify(escalaRepository).save(captor.capture());
        assertThat(captor.getValue().getMinisterioId()).isEqualTo(segundo.getId());
        verify(geracaoEscalaService).gerar(existenteDoPrimeiro);
        verify(geracaoEscalaService).gerar(captor.getValue());
    }

    @Test
    void naoDeveCriarEscalaDeMinisterioFinalizada() {
        Ministerio ministerio = ministerio();
        Escala existente = escala();
        existente.setTipo(TipoEscala.MINISTERIO);
        existente.setMinisterioId(ministerio.getId());
        existente.setStatus(StatusEscala.FINALIZADA);

        when(igrejaRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(igreja()));
        prepararEventosNoMes();
        when(ministerioRepository.findByIgrejaIdOrderByNomeAsc(igrejaId)).thenReturn(List.of(ministerio));
        when(escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipo(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(servicoRepository.findByMinisterioIdInAndAtivoTrueOrderByNomeAsc(anyList())).thenReturn(List.of());
        when(escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipoAndMinisterioId(
                any(), any(), any(), eq(ministerio.getId()))).thenReturn(Optional.of(existente));

        String resumo = escalaAutomacaoService.gerarProximoMes();

        assertThat(resumo).contains("0 criadas, 0 atualizadas");
        verify(escalaRepository, never()).save(any(Escala.class));
        verify(geracaoEscalaService, never()).gerar(any(Escala.class));
    }

    @Test
    void deveIgnorarServicoInativoEFiccaoAoGerarEscalaDeMinisterio() {
        Ministerio ministerio = ministerio();
        Servico inativo = servico();
        inativo.setAtivo(false);
        Servico ficticio = servico();
        ficticio.setFiccao(true);
        Servico valido = servico();

        when(igrejaRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(igreja()));
        prepararEventosNoMes();
        when(ministerioRepository.findByIgrejaIdOrderByNomeAsc(igrejaId)).thenReturn(List.of(ministerio));
        when(escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipo(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(servicoRepository.findByMinisterioIdInAndAtivoTrueOrderByNomeAsc(anyList())).thenReturn(List.of());
        when(escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipoAndMinisterioId(
                any(), any(), any(), eq(ministerio.getId()))).thenReturn(Optional.empty());
        when(servicoRepository.findByMinisterioIdOrderByNomeAsc(ministerio.getId()))
                .thenReturn(List.of(inativo, ficticio, valido));
        voluntariosAtivos(inativo.getId(), List.of(pessoaServico(inativo.getId())));
        voluntariosAtivos(ficticio.getId(), List.of(pessoaServico(ficticio.getId())));
        voluntariosAtivos(valido.getId(), List.of(pessoaServico(valido.getId())));
        when(escalaRepository.save(any(Escala.class))).thenAnswer(inv -> {
            Escala e = inv.getArgument(0);
            e.setId(escalaId);
            return e;
        });

        escalaAutomacaoService.gerarProximoMes();

        ArgumentCaptor<EscalaServico> captor = ArgumentCaptor.forClass(EscalaServico.class);
        verify(escalaServicoRepository).save(captor.capture());
        assertThat(captor.getValue().getServicoId()).isEqualTo(valido.getId());
        assertThat(captor.getValue().getQuantidadeNecessaria()).isEqualTo(1);
    }

    @Test
    void naoDeveCriarEscalaDeCultoComServicosFicticiosOuSemVoluntario() {
        Servico ficticio = servico();
        ficticio.setFiccao(true);
        Servico semVoluntario = servico();

        when(igrejaRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(igreja()));
        prepararEventosNoMes();
        when(ministerioRepository.findByIgrejaIdOrderByNomeAsc(igrejaId)).thenReturn(List.of());
        when(escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipo(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(servicoRepository.findByMinisterioIdInAndAtivoTrueOrderByNomeAsc(anyList()))
                .thenReturn(List.of(ficticio, semVoluntario));
        voluntariosAtivos(ficticio.getId(), List.of(pessoaServico(ficticio.getId())));
        voluntariosAtivos(semVoluntario.getId(), List.of());

        String resumo = escalaAutomacaoService.gerarProximoMes();

        assertThat(resumo).contains("0 criadas, 0 atualizadas");
        verify(escalaRepository, never()).save(any(Escala.class));
    }

    @Test
    void deveGerarEscalaComPastorComoSupervisorPregadorETituloDoMes() {
        String competencia = YearMonth.now().plusMonths(1).toString();
        Igreja igreja = igreja();
        Servico servicoCulto = servico();

        when(igrejaRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(igreja));
        prepararEventosNoMes();
        when(ministerioRepository.findByIgrejaIdOrderByNomeAsc(igrejaId)).thenReturn(List.of());
        when(escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipo(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(servicoRepository.findByMinisterioIdInAndAtivoTrueOrderByNomeAsc(anyList()))
                .thenReturn(List.of(servicoCulto));
        voluntariosAtivos(servicoCulto.getId(), List.of(pessoaServico(servicoCulto.getId())));
        when(escalaRepository.save(any(Escala.class))).thenAnswer(inv -> {
            Escala e = inv.getArgument(0);
            e.setId(escalaId);
            return e;
        });

        escalaAutomacaoService.gerarProximoMes();

        ArgumentCaptor<Escala> captor = ArgumentCaptor.forClass(Escala.class);
        verify(escalaRepository).save(captor.capture());
        Escala criada = captor.getValue();
        assertThat(criada.getSupervisorPessoaId()).isEqualTo(igreja.getPastorPessoaId());
        assertThat(criada.getPregadorPessoaId()).isEqualTo(igreja.getPastorPessoaId());
        assertThat(criada.getDataEvento()).isEqualTo(YearMonth.parse(competencia).atDay(1));
        assertThat(criada.getHoraInicio()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(criada.getAtivo()).isTrue();
        assertThat(criada.getTitulo()).startsWith("Escala de culto — ");
        assertThat(criada.getTitulo()).endsWith(YearMonth.parse(competencia).getYear() + "");
    }

    @Test
    void deveProcessarTodasAsIgrejasAtivas() {
        Igreja segunda = igreja();
        segunda.setId(UUID.randomUUID());
        Servico servicoCulto = servico();

        when(igrejaRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(igreja(), segunda));
        prepararEventosNoMes();
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                eq(List.of(segunda.getId())), any(), any())).thenReturn(List.of());
        when(ministerioRepository.findByIgrejaIdOrderByNomeAsc(igrejaId)).thenReturn(List.of());
        when(escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipo(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(servicoRepository.findByMinisterioIdInAndAtivoTrueOrderByNomeAsc(anyList()))
                .thenReturn(List.of(servicoCulto));
        voluntariosAtivos(servicoCulto.getId(), List.of(pessoaServico(servicoCulto.getId())));
        when(escalaRepository.save(any(Escala.class))).thenAnswer(inv -> {
            Escala e = inv.getArgument(0);
            e.setId(escalaId);
            return e;
        });

        String resumo = escalaAutomacaoService.gerarProximoMes();

        assertThat(resumo).contains("1 criadas");
        verify(ministerioRepository, never()).findByIgrejaIdOrderByNomeAsc(segunda.getId());
    }

    @Test
    void gerarProximoMesManualDeveExigirPermissaoDeMatriz() {
        doThrow(new AccessDeniedException("Apenas matriz/pastor/administrador"))
                .when(autorizacaoService).assertMatrizPastorOuAdmin();

        assertThatThrownBy(() -> escalaAutomacaoService.gerarProximoMesManual())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("matriz");
        verify(igrejaRepository, never()).findByAtivoTrueOrderByNomeAsc();
        verify(escalaRepository, never()).save(any(Escala.class));
    }

    @Test
    void geracaoAgendadaNaoDeveExigirPermissaoDeUsuario() {
        when(igrejaRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of());

        String resumo = escalaAutomacaoService.gerarProximoMesAgendado();

        assertThat(resumo).contains(YearMonth.now().plusMonths(1).toString());
        assertThat(resumo).contains("0 criadas, 0 atualizadas");
        verify(autorizacaoService, never()).assertMatrizPastorOuAdmin();
    }

    private Ministerio ministerio() {
        Ministerio ministerio = new Ministerio();
        ministerio.setId(UUID.randomUUID());
        ministerio.setNome("Louvor");
        ministerio.setIgrejaId(igrejaId);
        return ministerio;
    }

    private PessoaServico pessoaServico(UUID servicoRelacionado) {
        PessoaServico vinculo = new PessoaServico();
        vinculo.setPessoaId(UUID.randomUUID());
        vinculo.setServicoId(servicoRelacionado);
        vinculo.setAtivo(true);
        return vinculo;
    }

    private void prepararEventosNoMes() {
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

    private void voluntariosAtivos(UUID servicoId, List<PessoaServico> vinculos) {
        vinculos.forEach(v -> v.setServicoId(servicoId));
        voluntariosPorServico.put(servicoId, vinculos);
    }
}
