package com.example.base_api.escala.service;

import com.example.base_api.cronograma.entity.CronogramaEvento;
import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import com.example.base_api.disponibilidade.entity.DiaSemana;
import com.example.base_api.disponibilidade.entity.DisponibilidadeSemanal;
import com.example.base_api.disponibilidade.entity.Indisponibilidade;
import com.example.base_api.disponibilidade.repository.DisponibilidadeSemanalRepository;
import com.example.base_api.disponibilidade.repository.IndisponibilidadeRepository;
import com.example.base_api.escala.dto.GeracaoEscalaResponse;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.EscalaServico;
import com.example.base_api.escala.entity.EscalaVoluntario;
import com.example.base_api.escala.entity.StatusEscalaVoluntario;
import com.example.base_api.escala.entity.TipoEscala;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaServicoRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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

    private void prepararServicoDaEscala() {
        EscalaServico necessidade = new EscalaServico();
        necessidade.setEscalaId(escalaId);
        necessidade.setServicoId(servicoId);
        necessidade.setQuantidadeNecessaria(1);
        when(escalaServicoRepository.findByEscalaId(escalaId)).thenReturn(List.of(necessidade));
        Servico servico = new Servico();
        servico.setId(servicoId);
        servico.setNome("Violão");
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico);
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

    private Pessoa pessoa(UUID id, String nome) {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(id);
        pessoa.setNome(nome);
        pessoa.setIgrejaId(igrejaId);
        pessoa.setAtivo(true);
        return pessoa;
    }
}