package com.example.base_api.escala.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.cronograma.entity.CronogramaEvento;
import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import com.example.base_api.disponibilidade.entity.DiaSemana;
import com.example.base_api.disponibilidade.entity.DisponibilidadeSemanal;
import com.example.base_api.disponibilidade.entity.Indisponibilidade;
import com.example.base_api.disponibilidade.repository.DisponibilidadeSemanalRepository;
import com.example.base_api.disponibilidade.repository.IndisponibilidadeRepository;
import com.example.base_api.escala.dto.CandidatoDisponibilidadeResponse;
import com.example.base_api.escala.dto.GeracaoEscalaResponse;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.EscalaServico;
import com.example.base_api.escala.entity.EscalaVoluntario;
import com.example.base_api.escala.entity.StatusEscalaVoluntario;
import com.example.base_api.escala.entity.TipoEscala;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaServicoRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.entity.PessoaPausa;
import com.example.base_api.pessoa.repository.PessoaPausaRepository;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.service.ServicoService;
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
import java.util.List;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeracaoEscalaServiceTest {

    @Mock
    private EscalaRepository escalaRepository;
    @Mock
    private EscalaServicoRepository escalaServicoRepository;
    @Mock
    private EscalaVoluntarioRepository escalaVoluntarioRepository;
    @Mock
    private CronogramaEventoRepository cronogramaEventoRepository;
    @Mock
    private PessoaServicoRepository pessoaServicoRepository;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private PessoaPausaRepository pessoaPausaRepository;
    @Mock
    private DisponibilidadeSemanalRepository disponibilidadeSemanalRepository;
    @Mock
    private IndisponibilidadeRepository indisponibilidadeRepository;
    @Mock
    private ServicoService servicoService;
    @Mock
    private MinisterioRepository ministerioRepository;
    @Mock
    private AutorizacaoService autorizacaoService;

    @InjectMocks
    private GeracaoEscalaService geracaoEscalaService;

    private UUID escalaId;
    private UUID servicoId;
    private UUID igrejaId;
    private UUID pessoaA;
    private UUID pessoaB;
    private LocalDate inicioAgosto;
    private LocalDate fimAgosto;

    @BeforeEach
    void setUp() {
        escalaId = UUID.randomUUID();
        servicoId = UUID.randomUUID();
        igrejaId = UUID.randomUUID();
        pessoaA = UUID.randomUUID();
        pessoaB = UUID.randomUUID();
        inicioAgosto = YearMonth.of(2026, 8).atDay(1);
        fimAgosto = YearMonth.of(2026, 8).atEndOfMonth();
    }

    @Test
    void devePreencherVagaPorCultoComMenorCarga() {
        Escala escala = escala();
        prepararServicoDaEscala();
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), eq(inicioAgosto), eq(fimAgosto)))
                .thenReturn(List.of(evento(LocalDate.of(2026, 8, 2)), evento(LocalDate.of(2026, 8, 9))));
        prepararVoluntariosAtivos();
        prepararSemRestricoes();
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByPessoaIdInAndDataItemBetween(anyList(), any(), any())).thenReturn(List.of());

        GeracaoEscalaResponse response = geracaoEscalaService.gerar(escala);

        assertThat(response.status()).isEqualTo("COMPLETA");
        ArgumentCaptor<List<EscalaVoluntario>> captor = ArgumentCaptor.forClass(List.class);
        verify(escalaVoluntarioRepository).saveAll(captor.capture());
        List<EscalaVoluntario> itens = captor.getValue();
        assertThat(itens).hasSize(2);
        assertThat(itens).extracting(EscalaVoluntario::getDataItem)
                .containsExactly(LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 9));
        assertThat(itens.get(0).getPessoaId()).isNotEqualTo(itens.get(1).getPessoaId());
        assertThat(itens).allSatisfy(i -> {
            assertThat(i.getEscalaId()).isEqualTo(escalaId);
            assertThat(i.getServicoId()).isEqualTo(servicoId);
            assertThat(i.getStatus()).isEqualTo(StatusEscalaVoluntario.PENDENTE);
            assertThat(i.getHorario()).isEqualTo(LocalTime.of(18, 0));
        });
    }

    @Test
    void deveIgnorarVoluntarioIndisponivelNoDia() {
        Escala escala = escala();
        prepararServicoDaEscala();
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), eq(inicioAgosto), eq(fimAgosto)))
                .thenReturn(List.of(evento(LocalDate.of(2026, 8, 2))));
        prepararVoluntariosAtivos();
        Indisponibilidade indA = new Indisponibilidade();
        indA.setPessoaId(pessoaA);
        indA.setDataInicio(LocalDate.of(2026, 8, 1));
        indA.setDataFim(LocalDate.of(2026, 8, 3));
        when(indisponibilidadeRepository.findByPessoaIdIn(anyList())).thenReturn(List.of(indA));
        when(pessoaPausaRepository.findByPessoaIdInAndAtivaTrue(anyList())).thenReturn(List.of());
        when(disponibilidadeSemanalRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByPessoaIdInAndDataItemBetween(anyList(), any(), any())).thenReturn(List.of());

        GeracaoEscalaResponse response = geracaoEscalaService.gerar(escala);

        ArgumentCaptor<List<EscalaVoluntario>> captor = ArgumentCaptor.forClass(List.class);
        verify(escalaVoluntarioRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement()
                .extracting(EscalaVoluntario::getPessoaId)
                .isEqualTo(pessoaB);
        assertThat(response.status()).isEqualTo("COMPLETA");
    }

    @Test
    void deveIgnorarVoluntarioEmPausaAtiva() {
        Escala escala = escala();
        prepararServicoDaEscala();
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), eq(inicioAgosto), eq(fimAgosto)))
                .thenReturn(List.of(evento(LocalDate.of(2026, 8, 2))));
        prepararVoluntariosAtivos();
        when(indisponibilidadeRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        PessoaPausa pausaA = new PessoaPausa();
        pausaA.setPessoaId(pessoaA);
        pausaA.setDataInicio(LocalDate.of(2026, 8, 1));
        pausaA.setDataFim(LocalDate.of(2026, 8, 3));
        pausaA.setAtiva(true);
        when(pessoaPausaRepository.findByPessoaIdInAndAtivaTrue(anyList())).thenReturn(List.of(pausaA));
        when(disponibilidadeSemanalRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByPessoaIdInAndDataItemBetween(anyList(), any(), any())).thenReturn(List.of());

        geracaoEscalaService.gerar(escala);

        ArgumentCaptor<List<EscalaVoluntario>> captor = ArgumentCaptor.forClass(List.class);
        verify(escalaVoluntarioRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement()
                .extracting(EscalaVoluntario::getPessoaId)
                .isEqualTo(pessoaB);
    }

    @Test
    void deveIgnorarVoluntarioComConflitoEmOutraEscala() {
        Escala escala = escala();
        prepararServicoDaEscala();
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), eq(inicioAgosto), eq(fimAgosto)))
                .thenReturn(List.of(evento(LocalDate.of(2026, 8, 2))));
        prepararVoluntariosAtivos();
        when(indisponibilidadeRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(pessoaPausaRepository.findByPessoaIdInAndAtivaTrue(anyList())).thenReturn(List.of());
        when(disponibilidadeSemanalRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());

        EscalaVoluntario conflitoA = new EscalaVoluntario();
        conflitoA.setEscalaId(UUID.randomUUID());
        conflitoA.setPessoaId(pessoaA);
        conflitoA.setDataItem(LocalDate.of(2026, 8, 2));
        conflitoA.setHorario(LocalTime.of(18, 0));
        when(escalaVoluntarioRepository.findByPessoaIdInAndDataItemBetween(anyList(), any(), any()))
                .thenReturn(List.of(conflitoA));

        geracaoEscalaService.gerar(escala);

        ArgumentCaptor<List<EscalaVoluntario>> captor = ArgumentCaptor.forClass(List.class);
        verify(escalaVoluntarioRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement()
                .extracting(EscalaVoluntario::getPessoaId)
                .isEqualTo(pessoaB);
    }

    @Test
    void deveRespeitarDisponibilidadeSemanalCadastrada() {
        Escala escala = escala();
        prepararServicoDaEscala();
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), eq(inicioAgosto), eq(fimAgosto)))
                .thenReturn(List.of(evento(LocalDate.of(2026, 8, 6))));
        prepararVoluntariosAtivos();
        when(indisponibilidadeRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(pessoaPausaRepository.findByPessoaIdInAndAtivaTrue(anyList())).thenReturn(List.of());

        DisponibilidadeSemanal domingo = new DisponibilidadeSemanal();
        domingo.setPessoaId(pessoaA);
        domingo.setDiaSemana(DiaSemana.DOMINGO);
        domingo.setHoraInicio(LocalTime.of(18, 0));
        domingo.setHoraFim(LocalTime.of(21, 0));
        domingo.setDisponivel(true);
        when(disponibilidadeSemanalRepository.findByPessoaIdIn(anyList())).thenReturn(List.of(domingo));

        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByPessoaIdInAndDataItemBetween(anyList(), any(), any())).thenReturn(List.of());

        geracaoEscalaService.gerar(escala);

        ArgumentCaptor<List<EscalaVoluntario>> captor = ArgumentCaptor.forClass(List.class);
        verify(escalaVoluntarioRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement()
                .extracting(EscalaVoluntario::getPessoaId)
                .isEqualTo(pessoaB);
    }

    @Test
    void devePreservarDesignacoesExistentesSemDuplicar() {
        Escala escala = escala();
        prepararServicoDaEscala();
        CronogramaEvento evento = evento(LocalDate.of(2026, 8, 2));
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), eq(inicioAgosto), eq(fimAgosto)))
                .thenReturn(List.of(evento));
        prepararVoluntariosAtivos();
        when(indisponibilidadeRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(pessoaPausaRepository.findByPessoaIdInAndAtivaTrue(anyList())).thenReturn(List.of());
        when(disponibilidadeSemanalRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByPessoaIdInAndDataItemBetween(anyList(), any(), any())).thenReturn(List.of());

        EscalaVoluntario existente = new EscalaVoluntario();
        existente.setEscalaId(escalaId);
        existente.setPessoaId(pessoaA);
        existente.setServicoId(servicoId);
        existente.setDataItem(LocalDate.of(2026, 8, 2));
        existente.setEventoId(evento.getId());
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of(existente));

        geracaoEscalaService.gerar(escala);

        verify(escalaVoluntarioRepository, never()).saveAll(any());
    }

    @Test
    void deveDesignarPessoaFixaParaEscalaDePregador() {
        Escala escala = escala();
        escala.setTipo(TipoEscala.PREGADOR);
        escala.setPregadorPessoaId(pessoaA);
        when(escalaServicoRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), eq(inicioAgosto), eq(fimAgosto)))
                .thenReturn(List.of(evento(LocalDate.of(2026, 8, 2))));
        when(pessoaRepository.findAllById(anyList())).thenReturn(List.of(pessoa(pessoaA, "Ana")));
        when(indisponibilidadeRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(pessoaPausaRepository.findByPessoaIdInAndAtivaTrue(anyList())).thenReturn(List.of());
        when(disponibilidadeSemanalRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByPessoaIdInAndDataItemBetween(anyList(), any(), any())).thenReturn(List.of());

        geracaoEscalaService.gerar(escala);

        ArgumentCaptor<List<EscalaVoluntario>> captor = ArgumentCaptor.forClass(List.class);
        verify(escalaVoluntarioRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(item -> {
            assertThat(item.getPessoaId()).isEqualTo(pessoaA);
            assertThat(item.getServicoId()).isNull();
            assertThat(item.getDataItem()).isEqualTo(LocalDate.of(2026, 8, 2));
        });
    }

    @Test
    void deveRetornarSemEventosQuandoCronogramaVazio() {
        Escala escala = escala();
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), eq(inicioAgosto), eq(fimAgosto)))
                .thenReturn(List.of());

        GeracaoEscalaResponse response = geracaoEscalaService.gerar(escala);

        assertThat(response.status()).isEqualTo("SEM_EVENTOS");
        verify(escalaVoluntarioRepository, never()).saveAll(any());
    }

    @Test
    void deveGerarSugestaoSomenteParaOEventoInformado() {
        Escala escala = escala();
        prepararServicoDaEscala();
        prepararVoluntariosAtivos();
        prepararSemRestricoes();
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByPessoaIdInAndDataItemBetween(anyList(), any(), any())).thenReturn(List.of());

        CronogramaEvento evento = evento(LocalDate.of(2026, 8, 2));
        GeracaoEscalaResponse response = geracaoEscalaService.gerarParaEvento(escala, evento);

        assertThat(response.status()).isEqualTo("COMPLETA");
        ArgumentCaptor<List<EscalaVoluntario>> captor = ArgumentCaptor.forClass(List.class);
        verify(escalaVoluntarioRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(item -> {
            assertThat(item.getEventoId()).isEqualTo(evento.getId());
            assertThat(item.getDataItem()).isEqualTo(LocalDate.of(2026, 8, 2));
            assertThat(item.getPessoaId()).isEqualTo(pessoaA);
        });
        verify(cronogramaEventoRepository, never())
                .findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(anyList(), any(), any());
    }

    @Test
    void deveRetornarSemEventosQuandoEventoInativo() {
        Escala escala = escala();
        CronogramaEvento evento = evento(LocalDate.of(2026, 8, 2));
        evento.setAtivo(false);

        GeracaoEscalaResponse response = geracaoEscalaService.gerarParaEvento(escala, evento);

        assertThat(response.status()).isEqualTo("SEM_EVENTOS");
        verify(escalaVoluntarioRepository, never()).saveAll(any());
    }

    @Test
    void gerarSugestaoDeveAutorizarAntesDeGerar() {
        Escala escala = escala();
        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        doThrow(new AccessDeniedException("Escala já lançada não pode ser alterada"))
                .when(autorizacaoService)
                .assertPodeMutarEscala(TipoEscala.MINISTERIO, igrejaId, "2026-08", null);

        assertThatThrownBy(() -> geracaoEscalaService.gerarSugestao(escalaId))
                .isInstanceOf(AccessDeniedException.class);
        verify(cronogramaEventoRepository, never())
                .findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(anyList(), any(), any());
    }

    @Test
    void gerarSugestaoDeveFalharQuandoEscalaNaoExiste() {
        when(escalaRepository.findById(escalaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> geracaoEscalaService.gerarSugestao(escalaId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Escala não encontrada");
    }

    @Test
    void deveRetornarSemServicosQuandoNaoHaNecessidadeNemPessoaFixa() {
        Escala escala = escala();
        escala.setTipo(TipoEscala.CULTO);
        when(escalaServicoRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), eq(inicioAgosto), eq(fimAgosto)))
                .thenReturn(List.of(evento(LocalDate.of(2026, 8, 2))));

        GeracaoEscalaResponse response = geracaoEscalaService.gerar(escala);

        assertThat(response.status()).isEqualTo("SEM_SERVICOS");
        assertThat(response.sugestoes()).isEmpty();
        verify(escalaVoluntarioRepository, never()).saveAll(any());
    }

    @Test
    void deveRetornarSemDisponivelQuandoNenhumVoluntarioPodeAtender() {
        Escala escala = escala();
        prepararServicoDaEscala();
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), eq(inicioAgosto), eq(fimAgosto)))
                .thenReturn(List.of(evento(LocalDate.of(2026, 8, 2))));
        prepararVoluntariosAtivos();
        when(indisponibilidadeRepository.findByPessoaIdIn(anyList())).thenReturn(List.of(indisponibilidade(pessoaA)));
        when(pessoaPausaRepository.findByPessoaIdInAndAtivaTrue(anyList())).thenReturn(List.of());
        when(disponibilidadeSemanalRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByPessoaIdInAndDataItemBetween(anyList(), any(), any()))
                .thenReturn(List.of(conflito(pessoaB)));

        GeracaoEscalaResponse response = geracaoEscalaService.gerar(escala);

        assertThat(response.status()).isEqualTo("SEM_DISPONIVEL");
        assertThat(response.message()).contains("0 de 1 vagas preenchidas");
        verify(escalaVoluntarioRepository, never()).saveAll(any());
    }

    @Test
    void deveRetornarParcialQuandoFaltamVoluntariosParaPreencherTodasAsVagas() {
        Escala escala = escala();
        prepararServicoDaEscala(3);
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), eq(inicioAgosto), eq(fimAgosto)))
                .thenReturn(List.of(evento(LocalDate.of(2026, 8, 2)), evento(LocalDate.of(2026, 8, 9))));
        prepararVoluntariosAtivos();
        prepararSemRestricoes();
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByPessoaIdInAndDataItemBetween(anyList(), any(), any())).thenReturn(List.of());

        GeracaoEscalaResponse response = geracaoEscalaService.gerar(escala);

        assertThat(response.status()).isEqualTo("PARCIAL");
        assertThat(response.sugestoes()).hasSize(4);
        assertThat(response.message()).contains("4 de 6 vagas preenchidas em 2 culto(s)/evento(s)");
    }

    @Test
    void deveIgnorarEventosInativosNoCronograma() {
        Escala escala = escala();
        CronogramaEvento inativo = evento(LocalDate.of(2026, 8, 2));
        inativo.setAtivo(false);
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), eq(inicioAgosto), eq(fimAgosto)))
                .thenReturn(List.of(inativo));

        GeracaoEscalaResponse response = geracaoEscalaService.gerar(escala);

        assertThat(response.status()).isEqualTo("SEM_EVENTOS");
        assertThat(response.message()).contains("Nenhum culto ou evento cadastrado");
    }

    @Test
    void deveIgnorarPessoaInativaNoBanco() {
        Escala escala = escala();
        prepararServicoDaEscala();
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), eq(inicioAgosto), eq(fimAgosto)))
                .thenReturn(List.of(evento(LocalDate.of(2026, 8, 2))));
        when(pessoaServicoRepository.findByServicoIdInAndAtivoTrue(anyList()))
                .thenReturn(List.of(vinculo(pessoaA), vinculo(pessoaB)));
        Pessoa inativa = pessoa(pessoaA, "Ana");
        inativa.setAtivo(false);
        when(pessoaRepository.findAllById(anyList())).thenReturn(List.of(inativa, pessoa(pessoaB, "Bruno")));
        prepararSemRestricoes();
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByPessoaIdInAndDataItemBetween(anyList(), any(), any())).thenReturn(List.of());

        geracaoEscalaService.gerar(escala);

        ArgumentCaptor<List<EscalaVoluntario>> captor = ArgumentCaptor.forClass(List.class);
        verify(escalaVoluntarioRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement()
                .extracting(EscalaVoluntario::getPessoaId)
                .isEqualTo(pessoaB);
    }

    @Test
    void deveIgnorarPausaInativa() {
        Escala escala = escala();
        prepararServicoDaEscala();
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), eq(inicioAgosto), eq(fimAgosto)))
                .thenReturn(List.of(evento(LocalDate.of(2026, 8, 2))));
        prepararVoluntariosAtivos();
        when(indisponibilidadeRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        PessoaPausa pausaInativa = pausa(pessoaA);
        pausaInativa.setAtiva(false);
        when(pessoaPausaRepository.findByPessoaIdInAndAtivaTrue(anyList())).thenReturn(List.of(pausaInativa));
        when(disponibilidadeSemanalRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByPessoaIdInAndDataItemBetween(anyList(), any(), any())).thenReturn(List.of());

        geracaoEscalaService.gerar(escala);

        ArgumentCaptor<List<EscalaVoluntario>> captor = ArgumentCaptor.forClass(List.class);
        verify(escalaVoluntarioRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement()
                .extracting(EscalaVoluntario::getPessoaId)
                .isEqualTo(pessoaA);
    }

    @Test
    void deveIgnorarDisponibilidadeSemanalMarcadaComoIndisponivel() {
        Escala escala = escala();
        prepararServicoDaEscala();
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), eq(inicioAgosto), eq(fimAgosto)))
                .thenReturn(List.of(evento(LocalDate.of(2026, 8, 2))));
        prepararVoluntariosAtivos();
        when(indisponibilidadeRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(pessoaPausaRepository.findByPessoaIdInAndAtivaTrue(anyList())).thenReturn(List.of());
        DisponibilidadeSemanal foraDoHorario = new DisponibilidadeSemanal();
        foraDoHorario.setPessoaId(pessoaA);
        foraDoHorario.setDiaSemana(DiaSemana.DOMINGO);
        foraDoHorario.setHoraInicio(LocalTime.of(6, 0));
        foraDoHorario.setHoraFim(LocalTime.of(8, 0));
        foraDoHorario.setDisponivel(false);
        when(disponibilidadeSemanalRepository.findByPessoaIdIn(anyList())).thenReturn(List.of(foraDoHorario));
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByPessoaIdInAndDataItemBetween(anyList(), any(), any())).thenReturn(List.of());

        geracaoEscalaService.gerar(escala);

        ArgumentCaptor<List<EscalaVoluntario>> captor = ArgumentCaptor.forClass(List.class);
        verify(escalaVoluntarioRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement()
                .extracting(EscalaVoluntario::getPessoaId)
                .isEqualTo(pessoaB);
    }

    @Test
    void deveDesignarSupervisorFixoEmEscalaSemServicos() {
        Escala escala = escala();
        escala.setTipo(TipoEscala.SUPERVISOR);
        escala.setSupervisorPessoaId(pessoaB);
        when(escalaServicoRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), eq(inicioAgosto), eq(fimAgosto)))
                .thenReturn(List.of(evento(LocalDate.of(2026, 8, 2))));
        when(pessoaRepository.findAllById(anyList())).thenReturn(List.of(pessoa(pessoaB, "Bruno")));
        when(indisponibilidadeRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(pessoaPausaRepository.findByPessoaIdInAndAtivaTrue(anyList())).thenReturn(List.of());
        when(disponibilidadeSemanalRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByPessoaIdInAndDataItemBetween(anyList(), any(), any())).thenReturn(List.of());

        GeracaoEscalaResponse response = geracaoEscalaService.gerar(escala);

        assertThat(response.status()).isEqualTo("COMPLETA");
        assertThat(response.sugestoes()).singleElement().satisfies(sugestao -> {
            assertThat(sugestao.servicoNome()).isEqualTo("Supervisor");
            assertThat(sugestao.pessoaId()).isEqualTo(pessoaB);
        });
    }

    @Test
    void naoDeveDuplicarPessoaFixaJaDesignadaNoEvento() {
        Escala escala = escala();
        escala.setTipo(TipoEscala.PREGADOR);
        escala.setPregadorPessoaId(pessoaA);
        CronogramaEvento evento = evento(LocalDate.of(2026, 8, 2));
        when(escalaServicoRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                anyList(), eq(inicioAgosto), eq(fimAgosto)))
                .thenReturn(List.of(evento));
        when(pessoaRepository.findAllById(anyList())).thenReturn(List.of(pessoa(pessoaA, "Ana")));
        when(indisponibilidadeRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(pessoaPausaRepository.findByPessoaIdInAndAtivaTrue(anyList())).thenReturn(List.of());
        when(disponibilidadeSemanalRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByPessoaIdInAndDataItemBetween(anyList(), any(), any())).thenReturn(List.of());
        EscalaVoluntario existente = new EscalaVoluntario();
        existente.setEscalaId(escalaId);
        existente.setPessoaId(pessoaA);
        existente.setDataItem(LocalDate.of(2026, 8, 2));
        existente.setEventoId(evento.getId());
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of(existente));

        GeracaoEscalaResponse response = geracaoEscalaService.gerar(escala);

        assertThat(response.status()).isEqualTo("SEM_DISPONIVEL");
        verify(escalaVoluntarioRepository, never()).saveAll(any());
    }

    @Test
    void candidatosDeveNegarQuemNaoPodeVerEscala() {
        Escala escala = escala();
        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(autorizacaoService.podeVerEscala(escala)).thenReturn(false);

        assertThatThrownBy(() -> geracaoEscalaService.candidatos(escalaId, LocalDate.of(2026, 8, 2), null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Sem permissão para ver esta escala");
    }

    @Test
    void candidatosDeveRetornarVazioQuandoEscalaNaoTemServicos() {
        Escala escala = escala();
        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(autorizacaoService.podeVerEscala(escala)).thenReturn(true);
        when(escalaServicoRepository.findByEscalaId(escalaId)).thenReturn(List.of());

        assertThat(geracaoEscalaService.candidatos(escalaId, LocalDate.of(2026, 8, 2), null)).isEmpty();
    }

    @Test
    void candidatosDeveRejeitarServicoNaoPrevistoNaEscala() {
        Escala escala = escala();
        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(autorizacaoService.podeVerEscala(escala)).thenReturn(true);
        prepararServicoDaEscala();

        assertThatThrownBy(() -> geracaoEscalaService.candidatos(
                escalaId, LocalDate.of(2026, 8, 2), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Este serviço não está previsto nesta escala");
    }

    @Test
    void candidatosDeveRetornarVazioQuandoNenhumVinculoAtivo() {
        Escala escala = escala();
        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(autorizacaoService.podeVerEscala(escala)).thenReturn(true);
        prepararServicoDaEscala();
        when(pessoaServicoRepository.findByServicoIdInAndAtivoTrue(anyList())).thenReturn(List.of());

        assertThat(geracaoEscalaService.candidatos(escalaId, LocalDate.of(2026, 8, 2), null)).isEmpty();
    }

    @Test
    void candidatosDeveListarServicosEDiasDeDisponibilidade() {
        Escala escala = escala();
        LocalDate data = LocalDate.of(2026, 8, 2);
        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(autorizacaoService.podeVerEscala(escala)).thenReturn(true);
        prepararServicoDaEscala();
        when(pessoaServicoRepository.findByServicoIdInAndAtivoTrue(anyList())).thenReturn(List.of(vinculo(pessoaA)));
        when(pessoaRepository.findAllById(anyList())).thenReturn(List.of(pessoa(pessoaA, "Ana")));
        DisponibilidadeSemanal domingo = new DisponibilidadeSemanal();
        domingo.setPessoaId(pessoaA);
        domingo.setDiaSemana(DiaSemana.DOMINGO);
        domingo.setHoraInicio(LocalTime.of(18, 0));
        domingo.setHoraFim(LocalTime.of(21, 0));
        domingo.setDisponivel(true);
        when(disponibilidadeSemanalRepository.findByPessoaIdIn(anyList())).thenReturn(List.of(domingo));
        when(indisponibilidadeRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(pessoaPausaRepository.findByPessoaIdInAndAtivaTrue(anyList())).thenReturn(List.of());
        when(pessoaServicoRepository.findByPessoaIdIn(anyList())).thenReturn(List.of(vinculo(pessoaA)));

        List<CandidatoDisponibilidadeResponse> candidatos = geracaoEscalaService.candidatos(
                escalaId, data, servicoId);

        assertThat(candidatos).singleElement().satisfies(candidato -> {
            assertThat(candidato.pessoaId()).isEqualTo(pessoaA);
            assertThat(candidato.pessoaNome()).isEqualTo("Ana");
            assertThat(candidato.servicoNome()).isEqualTo("Violão");
            assertThat(candidato.servicosQueServe()).containsExactly("Violão");
            assertThat(candidato.diasDisponiveis()).singleElement()
                    .satisfies(dia -> assertThat(dia.diaSemana()).isEqualTo(DiaSemana.DOMINGO));
            assertThat(candidato.analiseDia()).isNull();
        });
    }

    @Test
    void candidatosDeveApontarIndisponibilidadeEConflitoDoDia() {
        Escala escala = escala();
        LocalDate data = LocalDate.of(2026, 8, 2);
        Ministerio ministerio = new Ministerio();
        ministerio.setId(UUID.randomUUID());
        ministerio.setNome("Louvor");
        Escala outra = escala();
        outra.setId(UUID.randomUUID());
        outra.setTitulo("Escala de Teens");
        outra.setMinisterioId(ministerio.getId());
        EscalaVoluntario conflito = conflito(pessoaA);
        conflito.setEscalaId(outra.getId());
        conflito.setServicoId(servicoId);

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(autorizacaoService.podeVerEscala(escala)).thenReturn(true);
        prepararServicoDaEscala();
        when(pessoaServicoRepository.findByServicoIdInAndAtivoTrue(anyList())).thenReturn(List.of(vinculo(pessoaA)));
        when(pessoaRepository.findAllById(anyList())).thenReturn(List.of(pessoa(pessoaA, "Ana")));
        when(disponibilidadeSemanalRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(indisponibilidadeRepository.findByPessoaIdIn(anyList())).thenReturn(List.of(indisponibilidade(pessoaA)));
        when(pessoaPausaRepository.findByPessoaIdInAndAtivaTrue(anyList())).thenReturn(List.of());
        when(pessoaServicoRepository.findByPessoaIdIn(anyList())).thenReturn(List.of(vinculo(pessoaA)));
        when(cronogramaEventoRepository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                eq(List.of(igrejaId)), eq(data), eq(data)))
                .thenReturn(List.of(evento(data)));
        when(escalaVoluntarioRepository.findByPessoaIdInAndDataItemBetween(anyList(), eq(data), eq(data)))
                .thenReturn(List.of(conflito));
        when(escalaRepository.findById(outra.getId())).thenReturn(Optional.of(outra));
        when(ministerioRepository.findById(ministerio.getId())).thenReturn(Optional.of(ministerio));

        List<CandidatoDisponibilidadeResponse> candidatos = geracaoEscalaService.candidatos(
                escalaId, data, servicoId);

        assertThat(candidatos).singleElement().satisfies(candidato -> {
            CandidatoDisponibilidadeResponse.AnaliseDia analise = candidato.analiseDia();
            assertThat(analise).isNotNull();
            assertThat(analise.disponivel()).isFalse();
            assertThat(analise.data()).isEqualTo(data);
            assertThat(analise.motivosIndisponibilidade())
                    .contains("Indisponível no dia (" + data + " a " + data + ")")
                    .anyMatch(m -> m.startsWith("Conflito com Escala de Teens"));
            assertThat(analise.conflitos()).singleElement().satisfies(c -> {
                assertThat(c.escalaTitulo()).isEqualTo("Escala de Teens");
                assertThat(c.ministerioNome()).isEqualTo("Louvor");
                assertThat(c.servicoNome()).isEqualTo("Violão");
            });
        });
    }

    private void prepararServicoDaEscala() {
        prepararServicoDaEscala(1);
    }

    private void prepararServicoDaEscala(int quantidade) {
        EscalaServico necessidade = new EscalaServico();
        necessidade.setEscalaId(escalaId);
        necessidade.setServicoId(servicoId);
        necessidade.setQuantidadeNecessaria(quantidade);
        when(escalaServicoRepository.findByEscalaId(escalaId)).thenReturn(List.of(necessidade));
        Servico servico = new Servico();
        servico.setId(servicoId);
        servico.setNome("Violão");
        lenient().when(servicoService.buscarEntidade(servicoId)).thenReturn(servico);
    }

    private void prepararVoluntariosAtivos() {
        PessoaServico vinculoA = new PessoaServico();
        vinculoA.setPessoaId(pessoaA);
        vinculoA.setServicoId(servicoId);
        vinculoA.setAtivo(true);
        PessoaServico vinculoB = new PessoaServico();
        vinculoB.setPessoaId(pessoaB);
        vinculoB.setServicoId(servicoId);
        vinculoB.setAtivo(true);
        when(pessoaServicoRepository.findByServicoIdInAndAtivoTrue(anyList()))
                .thenReturn(List.of(vinculoA, vinculoB));
        when(pessoaRepository.findAllById(anyList()))
                .thenReturn(List.of(pessoa(pessoaA, "Ana"), pessoa(pessoaB, "Bruno")));
    }

    private void prepararSemRestricoes() {
        when(indisponibilidadeRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
        when(pessoaPausaRepository.findByPessoaIdInAndAtivaTrue(anyList())).thenReturn(List.of());
        when(disponibilidadeSemanalRepository.findByPessoaIdIn(anyList())).thenReturn(List.of());
    }

    private Escala escala() {
        Escala escala = new Escala();
        escala.setId(escalaId);
        escala.setTitulo("Escala Louvor — agosto");
        escala.setCompetencia("2026-08");
        escala.setTipo(TipoEscala.MINISTERIO);
        escala.setIgrejaId(igrejaId);
        escala.setDataEvento(LocalDate.of(2026, 8, 1));
        escala.setHoraInicio(LocalTime.MIDNIGHT);
        escala.setAtivo(true);
        return escala;
    }

    private CronogramaEvento evento(LocalDate data) {
        CronogramaEvento evento = new CronogramaEvento();
        evento.setId(UUID.randomUUID());
        evento.setTitulo("Culto de domingo");
        evento.setDataEvento(data);
        evento.setHoraInicio(LocalTime.of(18, 0));
        evento.setHoraFim(LocalTime.of(20, 0));
        evento.setIgrejaId(igrejaId);
        evento.setAtivo(true);
        return evento;
    }

    private PessoaServico vinculo(UUID pessoaRelacionada) {
        PessoaServico vinculo = new PessoaServico();
        vinculo.setPessoaId(pessoaRelacionada);
        vinculo.setServicoId(servicoId);
        vinculo.setAtivo(true);
        return vinculo;
    }

    private Indisponibilidade indisponibilidade(UUID pessoaRelacionada) {
        Indisponibilidade ind = new Indisponibilidade();
        ind.setPessoaId(pessoaRelacionada);
        ind.setDataInicio(LocalDate.of(2026, 8, 2));
        ind.setDataFim(LocalDate.of(2026, 8, 2));
        ind.setHoraInicio(LocalTime.of(17, 0));
        ind.setHoraFim(LocalTime.of(21, 0));
        return ind;
    }

    private PessoaPausa pausa(UUID pessoaRelacionada) {
        PessoaPausa pausa = new PessoaPausa();
        pausa.setPessoaId(pessoaRelacionada);
        pausa.setDataInicio(LocalDate.of(2026, 8, 1));
        pausa.setDataFim(LocalDate.of(2026, 8, 31));
        pausa.setAtiva(true);
        return pausa;
    }

    private EscalaVoluntario conflito(UUID pessoaRelacionada) {
        EscalaVoluntario item = new EscalaVoluntario();
        item.setEscalaId(UUID.randomUUID());
        item.setPessoaId(pessoaRelacionada);
        item.setDataItem(LocalDate.of(2026, 8, 2));
        item.setHorario(LocalTime.of(18, 0));
        return item;
    }

    private Pessoa pessoa(UUID id, String nome) {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(id);
        pessoa.setNome(nome);
        pessoa.setIgrejaId(igrejaId);
        pessoa.setAtivo(true);
        return pessoa;
    }
}