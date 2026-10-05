package com.example.base_api.cronograma.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.cronograma.dto.CronogramaConteudoResponse;
import com.example.base_api.cronograma.dto.CronogramaConteudoUpdateRequest;
import com.example.base_api.cronograma.dto.CronogramaEventoCreateRequest;
import com.example.base_api.cronograma.dto.CronogramaEventoResponse;
import com.example.base_api.cronograma.dto.CronogramaEventoUpdateRequest;
import com.example.base_api.cronograma.entity.CronogramaConteudo;
import com.example.base_api.cronograma.entity.CronogramaEvento;
import com.example.base_api.cronograma.entity.EventoMinisterio;
import com.example.base_api.cronograma.entity.StatusEvento;
import com.example.base_api.cronograma.entity.TipoEvento;
import com.example.base_api.cronograma.repository.CronogramaConteudoRepository;
import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import com.example.base_api.cronograma.repository.EventoMinisterioRepository;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.EscalaServico;
import com.example.base_api.escala.entity.EscalaVoluntario;
import com.example.base_api.escala.entity.TipoEscala;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaServicoRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
import com.example.base_api.escala.service.GeracaoEscalaService;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.service.IgrejaContextoService;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.repository.ServicoRepository;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CronogramaServiceTest {

    @Mock
    private CronogramaEventoRepository repository;
    @Mock
    private CronogramaConteudoRepository conteudoRepository;
    @Mock
    private EventoMinisterioRepository eventoMinisterioRepository;
    @Mock
    private IgrejaContextoService igrejaContextoService;
    @Mock
    private AutorizacaoService autorizacaoService;
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
    private EscalaServicoRepository escalaServicoRepository;
    @Mock
    private EscalaVoluntarioRepository escalaVoluntarioRepository;
    @Mock
    private GeracaoEscalaService geracaoEscalaService;

    @InjectMocks
    private CronogramaService cronogramaService;

    @Test
    void deveCriarEventoNoCronograma() {
        UUID igrejaId = UUID.randomUUID();
        UUID pastorId = UUID.randomUUID();
        Igreja igreja = new Igreja();
        igreja.setId(igrejaId);
        igreja.setPastorPessoaId(pastorId);

        when(igrejaContextoService.buscarIgreja(igrejaId)).thenReturn(igreja);
        when(repository.save(any(CronogramaEvento.class))).thenAnswer(invocation -> {
            CronogramaEvento e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setCreatedAt(Instant.now());
            e.setUpdatedAt(Instant.now());
            return e;
        });

        CronogramaEventoResponse response = cronogramaService.criar(new CronogramaEventoCreateRequest(
                "Culto de Domingo",
                null,
                TipoEvento.CULTO,
                null,
                LocalDate.of(2026, 8, 24),
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                "Templo",
                igrejaId,
                null,
                null
        ));

        assertThat(response.titulo()).isEqualTo("Culto de Domingo");
        assertThat(response.tipo()).isEqualTo(TipoEvento.CULTO);
        assertThat(response.ativo()).isTrue();
        assertThat(response.igrejaId()).isEqualTo(igrejaId);
        assertThat(response.supervisorPessoaId()).isEqualTo(pastorId);
        assertThat(response.competencia()).isEqualTo("2026-08");
        assertThat(response.status()).isEqualTo(StatusEvento.AGENDADO);
    }

    @Test
    void deveCriarEventoComMinisteriosEGerarEscala() {
        UUID igrejaId = UUID.randomUUID();
        UUID pastorId = UUID.randomUUID();
        UUID ministerioId = UUID.randomUUID();
        UUID servicoId = UUID.randomUUID();

        Igreja igreja = new Igreja();
        igreja.setId(igrejaId);
        igreja.setPastorPessoaId(pastorId);

        Ministerio ministerio = new Ministerio();
        ministerio.setId(ministerioId);
        ministerio.setIgrejaId(igrejaId);
        ministerio.setNome("Louvor");

        Servico servico = new Servico();
        servico.setId(servicoId);
        servico.setMinisterioId(ministerioId);
        servico.setNome("Violão");
        servico.setAtivo(true);
        servico.setFiccao(false);

        PessoaServico vinculo = new PessoaServico();
        vinculo.setPessoaId(UUID.randomUUID());
        vinculo.setServicoId(servicoId);
        vinculo.setAtivo(true);

        when(igrejaContextoService.buscarIgreja(igrejaId)).thenReturn(igreja);
        when(repository.save(any(CronogramaEvento.class))).thenAnswer(invocation -> {
            CronogramaEvento e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setCreatedAt(Instant.now());
            e.setUpdatedAt(Instant.now());
            return e;
        });
        when(ministerioRepository.findById(ministerioId)).thenReturn(Optional.of(ministerio));
        when(eventoMinisterioRepository.findByEventoIdAndMinisterioId(any(), eq(ministerioId)))
                .thenReturn(Optional.empty());
        when(servicoRepository.findByMinisterioIdOrderByNomeAsc(ministerioId)).thenReturn(List.of(servico));
        when(pessoaServicoRepository.findByServicoIdAndAtivoTrue(servicoId)).thenReturn(List.of(vinculo));
        when(escalaRepository.findByEventoIdAndMinisterioId(any(), eq(ministerioId))).thenReturn(Optional.empty());
        when(escalaRepository.save(any(Escala.class))).thenAnswer(invocation -> {
            Escala e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(escalaServicoRepository.save(any(EscalaServico.class))).thenAnswer(invocation -> {
            EscalaServico es = invocation.getArgument(0);
            es.setId(UUID.randomUUID());
            return es;
        });

        EventoMinisterio participacao = new EventoMinisterio();
        participacao.setEventoId(UUID.randomUUID());
        participacao.setMinisterioId(ministerioId);
        when(eventoMinisterioRepository.findByEventoId(any())).thenReturn(List.of(participacao));

        Escala escala = new Escala();
        escala.setId(UUID.randomUUID());
        escala.setMinisterioId(ministerioId);
        escala.setTitulo("Louvor — Evento");
        when(escalaRepository.findByEventoId(any())).thenReturn(List.of(escala));

        EscalaServico necessidade = new EscalaServico();
        necessidade.setEscalaId(escala.getId());
        necessidade.setServicoId(servicoId);
        necessidade.setQuantidadeNecessaria(1);
        when(escalaServicoRepository.findByEscalaId(escala.getId())).thenReturn(List.of(necessidade));
        when(escalaVoluntarioRepository.findByEscalaId(escala.getId())).thenReturn(List.of());

        CronogramaEventoResponse response = cronogramaService.criar(new CronogramaEventoCreateRequest(
                "Show de Natal",
                "Natal em família",
                TipoEvento.EVENTO,
                "Apresentação especial",
                LocalDate.of(2026, 12, 20),
                LocalTime.of(19, 0),
                LocalTime.of(22, 0),
                "Templo",
                igrejaId,
                null,
                List.of(ministerioId)
        ));

        assertThat(response.tema()).isEqualTo("Natal em família");
        assertThat(response.competencia()).isEqualTo("2026-12");
        assertThat(response.status()).isEqualTo(StatusEvento.AGENDADO);
        assertThat(response.ministerios())
                .extracting(CronogramaEventoResponse.MinisterioParticipante::id)
                .containsExactly(ministerioId);
        assertThat(response.escalas()).hasSize(1);
        assertThat(response.escalas().get(0).vagasTotais()).isEqualTo(1);
        verify(geracaoEscalaService).gerarParaEvento(any(), any());
    }

    @Test
    void deveRejeitarMinisterioDeOutraIgreja() {
        UUID igrejaId = UUID.randomUUID();
        UUID pastorId = UUID.randomUUID();
        UUID ministerioId = UUID.randomUUID();

        Igreja igreja = new Igreja();
        igreja.setId(igrejaId);
        igreja.setPastorPessoaId(pastorId);

        Ministerio ministerio = new Ministerio();
        ministerio.setId(ministerioId);
        ministerio.setIgrejaId(UUID.randomUUID());

        when(igrejaContextoService.buscarIgreja(igrejaId)).thenReturn(igreja);
        when(repository.save(any(CronogramaEvento.class))).thenAnswer(invocation -> {
            CronogramaEvento e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setCreatedAt(Instant.now());
            e.setUpdatedAt(Instant.now());
            return e;
        });
        when(ministerioRepository.findById(ministerioId)).thenReturn(Optional.of(ministerio));

        assertThatThrownBy(() -> cronogramaService.criar(new CronogramaEventoCreateRequest(
                "Evento",
                null,
                TipoEvento.EVENTO,
                null,
                LocalDate.of(2026, 12, 20),
                LocalTime.of(19, 0),
                LocalTime.of(22, 0),
                null,
                igrejaId,
                null,
                List.of(ministerioId)
        ))).isInstanceOf(com.example.base_api.common.exception.BusinessException.class);
    }

    @Test
    void deveRetornarConteudoVazioQuandoNaoExiste() {
        UUID eventoId = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        UUID pastorId = UUID.randomUUID();
        CronogramaEvento evento = new CronogramaEvento();
        evento.setId(eventoId);
        evento.setIgrejaId(igrejaId);
        evento.setDataEvento(LocalDate.of(2026, 8, 9));

        Igreja igreja = new Igreja();
        igreja.setId(igrejaId);
        igreja.setPastorPessoaId(pastorId);

        when(repository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(conteudoRepository.findByEventoId(eventoId)).thenReturn(Optional.empty());
        when(escalaRepository.findByIgrejaIdAndCompetenciaAndTipo(any(), any(), any())).thenReturn(List.of());
        when(igrejaContextoService.buscarIgreja(igrejaId)).thenReturn(igreja);

        CronogramaConteudoResponse response = cronogramaService.obterConteudo(eventoId);

        assertThat(response.eventoId()).isEqualTo(eventoId);
        assertThat(response.textoPregacao()).isEmpty();
        assertThat(response.louvor()).isEmpty();
        assertThat(response.pregadorPessoaId()).isEqualTo(pastorId);
    }

    @Test
    void deveSalvarConteudoDoCulto() {
        UUID eventoId = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        CronogramaEvento evento = new CronogramaEvento();
        evento.setId(eventoId);
        evento.setIgrejaId(igrejaId);
        evento.setDataEvento(LocalDate.of(2026, 8, 9));

        when(repository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(conteudoRepository.findByEventoId(eventoId)).thenReturn(Optional.empty());
        when(autorizacaoService.podeEditarTexto(igrejaId, null)).thenReturn(true);
        when(autorizacaoService.podeEditarLouvor(igrejaId)).thenReturn(true);
        when(escalaRepository.findByIgrejaIdAndCompetenciaAndTipo(any(), any(), any())).thenReturn(List.of());
        Igreja igreja = new Igreja();
        igreja.setId(igrejaId);
        when(igrejaContextoService.buscarIgreja(igrejaId)).thenReturn(igreja);
        when(conteudoRepository.save(any(CronogramaConteudo.class))).thenAnswer(invocation -> {
            CronogramaConteudo c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CronogramaConteudoResponse response = cronogramaService.salvarConteudo(
                eventoId,
                new CronogramaConteudoUpdateRequest("João 15", "Bondade de Deus", null)
        );

        assertThat(response.textoPregacao()).isEqualTo("João 15");
        assertThat(response.louvor()).isEqualTo("Bondade de Deus");
    }

    @Test
    void spoofDePregadorNaoLiberaTexto() {
        UUID eventoId = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        UUID pregadorPersistido = UUID.randomUUID();
        UUID spoofId = UUID.randomUUID();

        CronogramaEvento evento = new CronogramaEvento();
        evento.setId(eventoId);
        evento.setIgrejaId(igrejaId);
        evento.setDataEvento(LocalDate.of(2026, 8, 9));

        CronogramaConteudo conteudo = new CronogramaConteudo();
        conteudo.setEventoId(eventoId);
        conteudo.setPregadorPessoaId(pregadorPersistido);
        conteudo.setTextoPregacao("texto original");

        when(repository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(conteudoRepository.findByEventoId(eventoId)).thenReturn(Optional.of(conteudo));
        when(autorizacaoService.podeEditarTexto(igrejaId, pregadorPersistido)).thenReturn(false);
        when(autorizacaoService.podeEditarLouvor(igrejaId)).thenReturn(false);

        assertThatThrownBy(() -> cronogramaService.salvarConteudo(
                eventoId,
                new CronogramaConteudoUpdateRequest("texto spoof", null, spoofId)
        )).isInstanceOf(AccessDeniedException.class);

        verify(autorizacaoService).podeEditarTexto(igrejaId, pregadorPersistido);
        verify(autorizacaoService, never()).podeEditarTexto(igrejaId, spoofId);
        verify(conteudoRepository, never()).save(any());
    }

    @Test
    void deveListarEventosDoPeriodoInformado() {
        UUID igrejaId = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 8, 1);
        LocalDate fim = LocalDate.of(2026, 8, 31);
        CronogramaEvento evento = evento(igrejaId, LocalDate.of(2026, 8, 16));

        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(repository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                List.of(igrejaId), inicio, fim)).thenReturn(List.of(evento));
        when(eventoMinisterioRepository.findByEventoId(evento.getId())).thenReturn(List.of());
        when(escalaRepository.findByEventoId(evento.getId())).thenReturn(List.of());

        List<CronogramaEventoResponse> eventos = cronogramaService.listar(igrejaId, inicio, fim);

        assertThat(eventos).singleElement()
                .extracting(CronogramaEventoResponse::titulo)
                .isEqualTo("Culto de domingo");
    }

    @Test
    void deveListarTodosOsEventosQuandoPeriodoNaoInformado() {
        UUID igrejaId = UUID.randomUUID();
        CronogramaEvento evento = evento(igrejaId, LocalDate.of(2026, 9, 6));

        when(autorizacaoService.idsParaListagem(null)).thenReturn(List.of(igrejaId));
        when(repository.findByIgrejaIdInOrderByDataEventoAscHoraInicioAsc(List.of(igrejaId)))
                .thenReturn(List.of(evento));
        when(eventoMinisterioRepository.findByEventoId(evento.getId())).thenReturn(List.of());
        when(escalaRepository.findByEventoId(evento.getId())).thenReturn(List.of());

        List<CronogramaEventoResponse> eventos = cronogramaService.listar(null, null, null);

        assertThat(eventos).hasSize(1);
        verify(repository, never())
                .findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(any(), any(), any());
    }

    @Test
    void naoDeveListarEventosQuandoNaoHaIgrejasNoEscopo() {
        UUID igrejaId = UUID.randomUUID();
        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of());

        assertThat(cronogramaService.listar(igrejaId, null, null)).isEmpty();
        verify(repository, never()).findByIgrejaIdInOrderByDataEventoAscHoraInicioAsc(any());
    }

    @Test
    void deveRejeitarPeriodoComDataFimAnteriorAoInicio() {
        UUID igrejaId = UUID.randomUUID();
        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));

        assertThatThrownBy(() -> cronogramaService.listar(
                igrejaId, LocalDate.of(2026, 8, 31), LocalDate.of(2026, 8, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Data fim deve ser igual ou posterior");
    }

    @Test
    void deveRejeitarConsultaDeEventoInexistente() {
        UUID eventoId = UUID.randomUUID();
        when(repository.findById(eventoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cronogramaService.buscarPorId(eventoId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Evento do cronograma não encontrado");
    }

    @Test
    void deveBuscarEventoResumindoVagasDasEscalasVinculadas() {
        UUID igrejaId = UUID.randomUUID();
        CronogramaEvento evento = evento(igrejaId, LocalDate.of(2026, 8, 16));
        Escala escala = escalaDeMinisterio(evento.getId(), igrejaId);

        when(repository.findById(evento.getId())).thenReturn(Optional.of(evento));
        when(eventoMinisterioRepository.findByEventoId(evento.getId())).thenReturn(List.of());
        when(escalaRepository.findByEventoId(evento.getId())).thenReturn(List.of(escala));
        when(escalaServicoRepository.findByEscalaId(escala.getId())).thenReturn(List.of(
                necessidade(escala.getId(), 2), necessidade(escala.getId(), 1)));
        when(escalaVoluntarioRepository.findByEscalaId(escala.getId())).thenReturn(List.of());

        CronogramaEventoResponse response = cronogramaService.buscarPorId(evento.getId());

        verify(autorizacaoService).assertAcesso(igrejaId);
        assertThat(response.escalas()).singleElement().satisfies(resumo -> {
            assertThat(resumo.vagasTotais()).isEqualTo(3);
            assertThat(resumo.vagasPreenchidas()).isZero();
            assertThat(resumo.titulo()).isEqualTo("Escala do evento");
        });
    }

    @Test
    void deveAtualizarEventoRecalculandoCompetencia() {
        UUID igrejaId = UUID.randomUUID();
        CronogramaEvento evento = evento(igrejaId, LocalDate.of(2026, 8, 16));
        when(repository.findById(evento.getId())).thenReturn(Optional.of(evento));
        when(repository.save(any(CronogramaEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventoMinisterioRepository.findByEventoId(evento.getId())).thenReturn(List.of());
        when(escalaRepository.findByEventoId(evento.getId())).thenReturn(List.of());

        CronogramaEventoResponse response = cronogramaService.atualizar(evento.getId(), atualizacao(
                LocalDate.of(2026, 9, 20), StatusEvento.ABERTO, true, null));

        verify(autorizacaoService).assertPastorOuAdmin(igrejaId);
        assertThat(response.competencia()).isEqualTo("2026-09");
        assertThat(response.status()).isEqualTo(StatusEvento.ABERTO);
        assertThat(response.titulo()).isEqualTo("Culto de domingo atualizado");
    }

    @Test
    void devePermitirCancelarEventoAgendado() {
        UUID igrejaId = UUID.randomUUID();
        CronogramaEvento evento = evento(igrejaId, LocalDate.of(2026, 8, 16));
        when(repository.findById(evento.getId())).thenReturn(Optional.of(evento));
        when(repository.save(any(CronogramaEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventoMinisterioRepository.findByEventoId(evento.getId())).thenReturn(List.of());
        when(escalaRepository.findByEventoId(evento.getId())).thenReturn(List.of());

        CronogramaEventoResponse response = cronogramaService.atualizar(evento.getId(), atualizacao(
                evento.getDataEvento(), StatusEvento.CANCELADO, false, null));

        assertThat(response.status()).isEqualTo(StatusEvento.CANCELADO);
        assertThat(response.ativo()).isFalse();
    }

    @Test
    void deveRealizarEventoAberto() {
        UUID igrejaId = UUID.randomUUID();
        CronogramaEvento evento = evento(igrejaId, LocalDate.of(2026, 8, 16));
        evento.setStatus(StatusEvento.ABERTO);
        when(repository.findById(evento.getId())).thenReturn(Optional.of(evento));
        when(repository.save(any(CronogramaEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventoMinisterioRepository.findByEventoId(evento.getId())).thenReturn(List.of());
        when(escalaRepository.findByEventoId(evento.getId())).thenReturn(List.of());

        CronogramaEventoResponse response = cronogramaService.atualizar(evento.getId(), atualizacao(
                evento.getDataEvento(), StatusEvento.REALIZADO, true, null));

        assertThat(response.status()).isEqualTo(StatusEvento.REALIZADO);
    }

    @Test
    void naoDeveReabrirEventoRealizado() {
        UUID igrejaId = UUID.randomUUID();
        CronogramaEvento evento = evento(igrejaId, LocalDate.of(2026, 8, 16));
        evento.setStatus(StatusEvento.REALIZADO);
        when(repository.findById(evento.getId())).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> cronogramaService.atualizar(evento.getId(), atualizacao(
                evento.getDataEvento(), StatusEvento.ABERTO, true, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição de status inválida");
        verify(repository, never()).save(any(CronogramaEvento.class));
    }

    @Test
    void naoDeveReagendarEventoCancelado() {
        UUID igrejaId = UUID.randomUUID();
        CronogramaEvento evento = evento(igrejaId, LocalDate.of(2026, 8, 16));
        evento.setStatus(StatusEvento.CANCELADO);
        when(repository.findById(evento.getId())).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> cronogramaService.atualizar(evento.getId(), atualizacao(
                evento.getDataEvento(), StatusEvento.AGENDADO, true, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição de status inválida");
    }

    @Test
    void deveAceitarAtualizacaoMantendoOMesmoStatus() {
        UUID igrejaId = UUID.randomUUID();
        CronogramaEvento evento = evento(igrejaId, LocalDate.of(2026, 8, 16));
        when(repository.findById(evento.getId())).thenReturn(Optional.of(evento));
        when(repository.save(any(CronogramaEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventoMinisterioRepository.findByEventoId(evento.getId())).thenReturn(List.of());
        when(escalaRepository.findByEventoId(evento.getId())).thenReturn(List.of());

        CronogramaEventoResponse response = cronogramaService.atualizar(evento.getId(), atualizacao(
                evento.getDataEvento(), StatusEvento.AGENDADO, true, null));

        assertThat(response.status()).isEqualTo(StatusEvento.AGENDADO);
    }

    @Test
    void deveDesvincularMinisterioEInativarEscalaVinculada() {
        UUID igrejaId = UUID.randomUUID();
        UUID ministerioId = UUID.randomUUID();
        CronogramaEvento evento = evento(igrejaId, LocalDate.of(2026, 8, 16));

        EventoMinisterio vinculo = new EventoMinisterio();
        vinculo.setEventoId(evento.getId());
        vinculo.setMinisterioId(ministerioId);
        Escala escala = escalaDeMinisterio(evento.getId(), igrejaId);
        escala.setAtivo(true);

        when(repository.findById(evento.getId())).thenReturn(Optional.of(evento));
        when(repository.save(any(CronogramaEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(igrejaContextoService.buscarIgreja(igrejaId)).thenReturn(igreja(igrejaId, null));
        when(eventoMinisterioRepository.findByEventoId(evento.getId())).thenReturn(List.of(vinculo));
        when(escalaRepository.findByEventoIdAndMinisterioId(evento.getId(), ministerioId))
                .thenReturn(Optional.of(escala));
        when(escalaRepository.findByEventoId(evento.getId())).thenReturn(List.of());

        cronogramaService.atualizar(evento.getId(), atualizacao(
                evento.getDataEvento(), StatusEvento.AGENDADO, true, List.of()));

        verify(eventoMinisterioRepository).delete(vinculo);
        ArgumentCaptor<Escala> captor = ArgumentCaptor.forClass(Escala.class);
        verify(escalaRepository).save(captor.capture());
        assertThat(captor.getValue().getAtivo()).isFalse();
    }

    @Test
    void naoDeveDuplicarVinculoDeMinisterioJaParticipante() {
        UUID igrejaId = UUID.randomUUID();
        UUID ministerioId = UUID.randomUUID();
        UUID servicoId = UUID.randomUUID();
        CronogramaEvento evento = evento(igrejaId, LocalDate.of(2026, 8, 16));

        Ministerio ministerio = ministerio(ministerioId, igrejaId);
        Servico servico = servico(servicoId, ministerioId, true, false);
        EventoMinisterio vinculoExistente = new EventoMinisterio();
        vinculoExistente.setEventoId(evento.getId());
        vinculoExistente.setMinisterioId(ministerioId);

        when(igrejaContextoService.buscarIgreja(igrejaId)).thenReturn(igreja(igrejaId, null));
        when(repository.save(any(CronogramaEvento.class))).thenAnswer(invocation -> {
            CronogramaEvento e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setCreatedAt(Instant.now());
            e.setUpdatedAt(Instant.now());
            return e;
        });
        when(ministerioRepository.findById(ministerioId)).thenReturn(Optional.of(ministerio));
        when(eventoMinisterioRepository.findByEventoIdAndMinisterioId(any(), eq(ministerioId)))
                .thenReturn(Optional.of(vinculoExistente));
        when(eventoMinisterioRepository.findByEventoId(any())).thenReturn(List.of(vinculoExistente));
        when(escalaRepository.findByEventoIdAndMinisterioId(any(), eq(ministerioId)))
                .thenReturn(Optional.of(escalaDeMinisterio(UUID.randomUUID(), igrejaId)));
        when(escalaRepository.findByEventoId(any())).thenReturn(List.of());

        cronogramaService.criar(new CronogramaEventoCreateRequest(
                "Show de Natal", null, TipoEvento.EVENTO, null,
                LocalDate.of(2026, 12, 20), LocalTime.of(19, 0), LocalTime.of(22, 0),
                "Templo", igrejaId, null, List.of(ministerioId)));

        verify(eventoMinisterioRepository, never()).save(any(EventoMinisterio.class));
        verify(servicoRepository, never()).findByMinisterioIdOrderByNomeAsc(any());
        verify(escalaRepository, never()).save(any(Escala.class));
    }

    @Test
    void naoDeveGerarEscalaQuandoMinisterioNaoTemServicoComVoluntario() {
        UUID igrejaId = UUID.randomUUID();
        UUID ministerioId = UUID.randomUUID();
        UUID servicoId = UUID.randomUUID();

        Ministerio ministerio = ministerio(ministerioId, igrejaId);
        Servico servico = servico(servicoId, ministerioId, true, false);

        when(igrejaContextoService.buscarIgreja(igrejaId)).thenReturn(igreja(igrejaId, null));
        when(repository.save(any(CronogramaEvento.class))).thenAnswer(invocation -> {
            CronogramaEvento e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setCreatedAt(Instant.now());
            e.setUpdatedAt(Instant.now());
            return e;
        });
        when(ministerioRepository.findById(ministerioId)).thenReturn(Optional.of(ministerio));
        when(eventoMinisterioRepository.findByEventoIdAndMinisterioId(any(), eq(ministerioId)))
                .thenReturn(Optional.empty());
        when(eventoMinisterioRepository.findByEventoId(any())).thenReturn(List.of());
        when(escalaRepository.findByEventoIdAndMinisterioId(any(), eq(ministerioId))).thenReturn(Optional.empty());
        when(servicoRepository.findByMinisterioIdOrderByNomeAsc(ministerioId)).thenReturn(List.of(servico));
        when(pessoaServicoRepository.findByServicoIdAndAtivoTrue(servicoId)).thenReturn(List.of());
        when(escalaRepository.findByEventoId(any())).thenReturn(List.of());

        cronogramaService.criar(new CronogramaEventoCreateRequest(
                "Show de Natal", null, TipoEvento.EVENTO, null,
                LocalDate.of(2026, 12, 20), LocalTime.of(19, 0), LocalTime.of(22, 0),
                "Templo", igrejaId, null, List.of(ministerioId)));

        verify(eventoMinisterioRepository).save(any(EventoMinisterio.class));
        verify(escalaRepository, never()).save(any(Escala.class));
        verify(geracaoEscalaService, never()).gerarParaEvento(any(), any());
    }

    @Test
    void naoDeveGerarSegundaEscalaParaOMesmoMinisterioNoEvento() {
        UUID igrejaId = UUID.randomUUID();
        UUID ministerioId = UUID.randomUUID();
        Ministerio ministerio = ministerio(ministerioId, igrejaId);

        when(igrejaContextoService.buscarIgreja(igrejaId)).thenReturn(igreja(igrejaId, null));
        when(repository.save(any(CronogramaEvento.class))).thenAnswer(invocation -> {
            CronogramaEvento e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setCreatedAt(Instant.now());
            e.setUpdatedAt(Instant.now());
            return e;
        });
        when(ministerioRepository.findById(ministerioId)).thenReturn(Optional.of(ministerio));
        when(eventoMinisterioRepository.findByEventoIdAndMinisterioId(any(), eq(ministerioId)))
                .thenReturn(Optional.empty());
        when(eventoMinisterioRepository.findByEventoId(any())).thenReturn(List.of());
        when(escalaRepository.findByEventoIdAndMinisterioId(any(), eq(ministerioId)))
                .thenReturn(Optional.of(escalaDeMinisterio(UUID.randomUUID(), igrejaId)));
        when(escalaRepository.findByEventoId(any())).thenReturn(List.of());

        cronogramaService.criar(new CronogramaEventoCreateRequest(
                "Show de Natal", null, TipoEvento.EVENTO, null,
                LocalDate.of(2026, 12, 20), LocalTime.of(19, 0), LocalTime.of(22, 0),
                "Templo", igrejaId, null, List.of(ministerioId)));

        verify(escalaRepository, never()).save(any(Escala.class));
        verify(geracaoEscalaService, never()).gerarParaEvento(any(), any());
    }

    @Test
    void deveRejeitarHoraFimNaoPosteriorAoInicio() {
        assertThatThrownBy(() -> cronogramaService.criar(new CronogramaEventoCreateRequest(
                "Ensaio", null, TipoEvento.ENSAIO, null,
                LocalDate.of(2026, 8, 16), LocalTime.of(20, 0), LocalTime.of(20, 0),
                null, UUID.randomUUID(), null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Hora fim deve ser posterior");
    }

    @Test
    void deveRejeitarCriacaoSemIgrejaDoUsuarioNemDoRequest() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        when(autorizacaoService.requireUsuario()).thenReturn(usuario);

        assertThatThrownBy(() -> cronogramaService.criar(new CronogramaEventoCreateRequest(
                "Evento", null, TipoEvento.EVENTO, null,
                LocalDate.of(2026, 8, 16), LocalTime.of(18, 0), LocalTime.of(20, 0),
                null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("igrejaId é obrigatório");
    }

    @Test
    void naoDeveCriarEventoParaIgrejaForaDoAcessoDoUsuario() {
        UUID igrejaId = UUID.randomUUID();
        doThrow(new BusinessException("Sem permissão para criar evento nesta igreja"))
                .when(autorizacaoService).assertPastorOuAdmin(igrejaId);

        assertThatThrownBy(() -> cronogramaService.criar(new CronogramaEventoCreateRequest(
                "Evento", null, TipoEvento.EVENTO, null,
                LocalDate.of(2026, 8, 16), LocalTime.of(18, 0), LocalTime.of(20, 0),
                null, igrejaId, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Sem permissão para criar evento");
        verify(repository, never()).save(any(CronogramaEvento.class));
    }

    @Test
    void deveExcluirEvento() {
        UUID igrejaId = UUID.randomUUID();
        CronogramaEvento evento = evento(igrejaId, LocalDate.of(2026, 8, 16));
        when(repository.findById(evento.getId())).thenReturn(Optional.of(evento));

        cronogramaService.excluir(evento.getId());

        verify(autorizacaoService).assertPastorOuAdmin(igrejaId);
        verify(repository).delete(evento);
    }

    @Test
    void deveResolverPregadorAPartirDaEscalaDePregadorDoMes() {
        UUID eventoId = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        UUID pregadorId = UUID.randomUUID();
        CronogramaEvento evento = evento(igrejaId, LocalDate.of(2026, 8, 16));
        evento.setId(eventoId);

        Escala escalaPregador = new Escala();
        escalaPregador.setId(UUID.randomUUID());
        escalaPregador.setIgrejaId(igrejaId);

        EscalaVoluntario item = new EscalaVoluntario();
        item.setEscalaId(escalaPregador.getId());
        item.setPessoaId(pregadorId);
        item.setEventoId(eventoId);
        item.setDataItem(evento.getDataEvento());

        when(repository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(conteudoRepository.findByEventoId(eventoId)).thenReturn(Optional.empty());
        when(escalaRepository.findByIgrejaIdAndCompetenciaAndTipo(igrejaId, "2026-08", TipoEscala.PREGADOR))
                .thenReturn(List.of(escalaPregador));
        when(escalaVoluntarioRepository.findByEscalaId(escalaPregador.getId())).thenReturn(List.of(item));
        when(pessoaRepository.findById(pregadorId)).thenReturn(Optional.of(pessoa(pregadorId, "Pr. Daniel")));

        CronogramaConteudoResponse response = cronogramaService.obterConteudo(eventoId);

        verify(autorizacaoService).assertPodeVerConteudo(igrejaId, pregadorId);
        assertThat(response.pregadorPessoaId()).isEqualTo(pregadorId);
        assertThat(response.pregadorNome()).isEqualTo("Pr. Daniel");
    }

    @Test
    void naoDeveExibirConteudoParaQuemNaoPodeVer() {
        UUID eventoId = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        UUID pastorId = UUID.randomUUID();
        CronogramaEvento evento = evento(igrejaId, LocalDate.of(2026, 8, 16));
        evento.setId(eventoId);

        when(repository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(conteudoRepository.findByEventoId(eventoId)).thenReturn(Optional.empty());
        when(escalaRepository.findByIgrejaIdAndCompetenciaAndTipo(any(), any(), any())).thenReturn(List.of());
        when(igrejaContextoService.buscarIgreja(igrejaId)).thenReturn(igreja(igrejaId, pastorId));
        doThrow(new AccessDeniedException("Sem permissão para ver o conteúdo deste culto"))
                .when(autorizacaoService).assertPodeVerConteudo(igrejaId, pastorId);

        assertThatThrownBy(() -> cronogramaService.obterConteudo(eventoId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void naoDeveAlterarPregadorPorQuemNaoEPastorOuAdmin() {
        UUID eventoId = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        UUID pastorId = UUID.randomUUID();
        UUID novoPregadorId = UUID.randomUUID();
        CronogramaEvento evento = evento(igrejaId, LocalDate.of(2026, 8, 16));
        evento.setId(eventoId);

        when(repository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(conteudoRepository.findByEventoId(eventoId)).thenReturn(Optional.empty());
        when(escalaRepository.findByIgrejaIdAndCompetenciaAndTipo(any(), any(), any())).thenReturn(List.of());
        when(igrejaContextoService.buscarIgreja(igrejaId)).thenReturn(igreja(igrejaId, pastorId));
        when(autorizacaoService.isPastorOuAdmin(igrejaId)).thenReturn(false);
        when(autorizacaoService.podeEditarTexto(igrejaId, pastorId)).thenReturn(true);

        assertThatThrownBy(() -> cronogramaService.salvarConteudo(
                eventoId, new CronogramaConteudoUpdateRequest(null, null, novoPregadorId)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Somente pastor ou admin pode alterar o pregador");
        verify(conteudoRepository, never()).save(any());
    }

    @Test
    void naoDeveAceitarPregadorDeOutraIgrejaNoConteudo() {
        UUID eventoId = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        UUID pastorId = UUID.randomUUID();
        UUID novoPregadorId = UUID.randomUUID();
        CronogramaEvento evento = evento(igrejaId, LocalDate.of(2026, 8, 16));
        evento.setId(eventoId);
        Pessoa alheia = pessoa(novoPregadorId, "Visitante");
        alheia.setIgrejaId(UUID.randomUUID());

        when(repository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(conteudoRepository.findByEventoId(eventoId)).thenReturn(Optional.empty());
        when(escalaRepository.findByIgrejaIdAndCompetenciaAndTipo(any(), any(), any())).thenReturn(List.of());
        when(igrejaContextoService.buscarIgreja(igrejaId)).thenReturn(igreja(igrejaId, pastorId));
        when(autorizacaoService.isPastorOuAdmin(igrejaId)).thenReturn(true);
        when(autorizacaoService.podeEditarTexto(igrejaId, pastorId)).thenReturn(true);
        when(pessoaRepository.findById(novoPregadorId)).thenReturn(Optional.of(alheia));
        doThrow(new BusinessException("A pessoa não pertence a esta igreja"))
                .when(autorizacaoService).assertPessoaDaIgreja(alheia, igrejaId);

        assertThatThrownBy(() -> cronogramaService.salvarConteudo(
                eventoId, new CronogramaConteudoUpdateRequest(null, null, novoPregadorId)))
                .isInstanceOf(BusinessException.class);
        verify(conteudoRepository, never()).save(any());
    }

    @Test
    void pastorDeveTrocarPregadorEEditarConteudoDoCulto() {
        UUID eventoId = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        UUID pastorId = UUID.randomUUID();
        UUID novoPregadorId = UUID.randomUUID();
        CronogramaEvento evento = evento(igrejaId, LocalDate.of(2026, 8, 16));
        evento.setId(eventoId);
        Pessoa pregador = pessoa(novoPregadorId, "Pr. Marcos");

        when(repository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(conteudoRepository.findByEventoId(eventoId)).thenReturn(Optional.empty());
        when(escalaRepository.findByIgrejaIdAndCompetenciaAndTipo(any(), any(), any())).thenReturn(List.of());
        when(igrejaContextoService.buscarIgreja(igrejaId)).thenReturn(igreja(igrejaId, pastorId));
        when(autorizacaoService.isPastorOuAdmin(igrejaId)).thenReturn(true);
        when(autorizacaoService.podeEditarTexto(igrejaId, pastorId)).thenReturn(true);
        when(autorizacaoService.podeEditarLouvor(igrejaId)).thenReturn(true);
        when(pessoaRepository.findById(novoPregadorId)).thenReturn(Optional.of(pregador));
        when(conteudoRepository.save(any(CronogramaConteudo.class))).thenAnswer(invocation -> {
            CronogramaConteudo c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CronogramaConteudoResponse response = cronogramaService.salvarConteudo(
                eventoId, new CronogramaConteudoUpdateRequest("Salmos 23", "Adoração", novoPregadorId));

        assertThat(response.pregadorPessoaId()).isEqualTo(novoPregadorId);
        assertThat(response.pregadorNome()).isEqualTo("Pr. Marcos");
        assertThat(response.textoPregacao()).isEqualTo("Salmos 23");
        assertThat(response.louvor()).isEqualTo("Adoração");
    }

    @Test
    void naoDeveAplicarCamposQueOUsuarioNaoPodeEditar() {
        UUID eventoId = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        UUID pregadorId = UUID.randomUUID();
        CronogramaEvento evento = evento(igrejaId, LocalDate.of(2026, 8, 16));
        evento.setId(eventoId);

        CronogramaConteudo conteudo = new CronogramaConteudo();
        conteudo.setEventoId(eventoId);
        conteudo.setPregadorPessoaId(pregadorId);
        conteudo.setTextoPregacao("texto original");
        conteudo.setLouvor("louvor original");

        when(repository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(conteudoRepository.findByEventoId(eventoId)).thenReturn(Optional.of(conteudo));
        when(autorizacaoService.podeEditarTexto(igrejaId, pregadorId)).thenReturn(false);
        when(autorizacaoService.podeEditarLouvor(igrejaId)).thenReturn(true);
        when(conteudoRepository.save(any(CronogramaConteudo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CronogramaConteudoResponse response = cronogramaService.salvarConteudo(
                eventoId, new CronogramaConteudoUpdateRequest("texto tentado", "louvor novo", null));

        assertThat(response.textoPregacao()).isEqualTo("texto original");
        assertThat(response.louvor()).isEqualTo("louvor novo");
    }

    private CronogramaEventoUpdateRequest atualizacao(
            LocalDate data,
            StatusEvento status,
            Boolean ativo,
            List<UUID> ministerioIds
    ) {
        return new CronogramaEventoUpdateRequest(
                "Culto de domingo atualizado", null, TipoEvento.CULTO, null,
                data, LocalTime.of(18, 0), LocalTime.of(20, 0), "Templo",
                null, status, ativo, ministerioIds);
    }

    private CronogramaEvento evento(UUID igrejaId, LocalDate data) {
        CronogramaEvento evento = new CronogramaEvento();
        evento.setId(UUID.randomUUID());
        evento.setTitulo("Culto de domingo");
        evento.setTipo(TipoEvento.CULTO);
        evento.setDataEvento(data);
        evento.setHoraInicio(LocalTime.of(18, 0));
        evento.setHoraFim(LocalTime.of(20, 0));
        evento.setIgrejaId(igrejaId);
        evento.setCompetencia("2026-08");
        evento.setStatus(StatusEvento.AGENDADO);
        evento.setAtivo(true);
        evento.setCreatedAt(Instant.now());
        evento.setUpdatedAt(Instant.now());
        return evento;
    }

    private Escala escalaDeMinisterio(UUID eventoId, UUID igrejaId) {
        Escala escala = new Escala();
        escala.setId(UUID.randomUUID());
        escala.setTitulo("Escala do evento");
        escala.setEventoId(eventoId);
        escala.setIgrejaId(igrejaId);
        escala.setCompetencia("2026-08");
        escala.setTipo(TipoEscala.MINISTERIO);
        escala.setAtivo(true);
        return escala;
    }

    private EscalaServico necessidade(UUID escalaId, int quantidade) {
        EscalaServico necessidade = new EscalaServico();
        necessidade.setId(UUID.randomUUID());
        necessidade.setEscalaId(escalaId);
        necessidade.setServicoId(UUID.randomUUID());
        necessidade.setQuantidadeNecessaria(quantidade);
        return necessidade;
    }

    private Ministerio ministerio(UUID id, UUID igrejaId) {
        Ministerio ministerio = new Ministerio();
        ministerio.setId(id);
        ministerio.setIgrejaId(igrejaId);
        ministerio.setNome("Louvor");
        return ministerio;
    }

    private Servico servico(UUID id, UUID ministerioId, boolean ativo, boolean ficcao) {
        Servico servico = new Servico();
        servico.setId(id);
        servico.setMinisterioId(ministerioId);
        servico.setNome("Violão");
        servico.setAtivo(ativo);
        servico.setFiccao(ficcao);
        return servico;
    }

    private Igreja igreja(UUID igrejaId, UUID pastorPessoaId) {
        Igreja igreja = new Igreja();
        igreja.setId(igrejaId);
        igreja.setPastorPessoaId(pastorPessoaId);
        return igreja;
    }

    private Pessoa pessoa(UUID id, String nome) {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(id);
        pessoa.setNome(nome);
        pessoa.setAtivo(true);
        return pessoa;
    }
}