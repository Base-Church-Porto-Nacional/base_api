package com.example.base_api.igreja.service;

import com.example.base_api.cronograma.entity.CronogramaConteudo;
import com.example.base_api.cronograma.entity.CronogramaEvento;
import com.example.base_api.cronograma.entity.StatusEvento;
import com.example.base_api.cronograma.repository.CronogramaConteudoRepository;
import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import com.example.base_api.culto.dto.CultoConteudoResponse;
import com.example.base_api.culto.entity.CategoriaCulto;
import com.example.base_api.culto.entity.Culto;
import com.example.base_api.culto.entity.StatusCulto;
import com.example.base_api.culto.repository.CategoriaCultoRepository;
import com.example.base_api.culto.repository.CultoRelatorioRepository;
import com.example.base_api.culto.service.CultoService;
import com.example.base_api.culto.repository.CultoRepository;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.EscalaVoluntario;
import com.example.base_api.escala.entity.StatusEscalaVoluntario;
import com.example.base_api.escala.entity.TipoEscala;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import org.springframework.data.jpa.repository.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipacoesFuturasServiceTest {

    // 02/11/2026 01:30 UTC = 01/11/2026 22:30 em São Paulo: "hoje" é 01/11, não 02/11.
    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-11-02T01:30:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 1);
    private static final String MES_ATUAL = "2026-11";
    // Culto/evento já realizado ou cancelado (mesmo que hoje) é histórico: não muda.
    private static final List<StatusCulto> CULTO_ENCERRADO = List.of(StatusCulto.REALIZADO, StatusCulto.CANCELADO);
    private static final List<StatusEvento> EVENTO_ENCERRADO = List.of(StatusEvento.REALIZADO, StatusEvento.CANCELADO);

    @Mock
    private IgrejaRepository igrejaRepository;
    @Mock
    private EscalaRepository escalaRepository;
    @Mock
    private EscalaVoluntarioRepository escalaVoluntarioRepository;
    @Mock
    private CultoRepository cultoRepository;
    @Mock
    private CronogramaEventoRepository cronogramaEventoRepository;
    @Mock
    private CronogramaConteudoRepository cronogramaConteudoRepository;
    @Mock
    private CategoriaCultoRepository categoriaCultoRepository;

    private ParticipacoesFuturasService service;

    private final UUID origem = UUID.randomUUID();
    private final UUID pessoaId = UUID.randomUUID();
    private final UUID titularId = UUID.randomUUID();
    private final UUID outraPessoa = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ParticipacoesFuturasService(igrejaRepository, escalaRepository, escalaVoluntarioRepository,
                cultoRepository, categoriaCultoRepository, cronogramaEventoRepository, cronogramaConteudoRepository, RELOGIO);
        Igreja igreja = new Igreja();
        igreja.setId(origem);
        igreja.setPastorPessoaId(titularId);
        lenient().when(igrejaRepository.findById(origem)).thenReturn(Optional.of(igreja));
    }

    @Test
    void removeDesignacoesFuturasDeVoluntarioDaIgrejaDeOrigem() {
        EscalaVoluntario futura = new EscalaVoluntario();
        futura.setPessoaId(pessoaId);
        when(escalaVoluntarioRepository.findFuturasDaPessoaNaIgreja(pessoaId, origem, HOJE, MES_ATUAL))
                .thenReturn(List.of(futura));

        int ajustadas = service.ajustarNaSaida(pessoaId, origem);

        // A vaga fica aberta para substituição (mesmo efeito de remover a designação na escala).
        verify(escalaVoluntarioRepository).deleteAll(List.of(futura));
        assertThat(ajustadas).isEqualTo(1);
    }

    @Test
    void consultasUsamHojeNoFusoConfiguradoEOMesAtual() {
        service.ajustarNaSaida(pessoaId, origem);

        // Registros passados ficam de fora já na consulta (data >= hoje / competência >= mês atual), assim como
        // cultos e eventos já realizados ou cancelados (status NOT IN).
        verify(escalaVoluntarioRepository).findFuturasDaPessoaNaIgreja(pessoaId, origem, HOJE, MES_ATUAL);
        verify(escalaRepository).findFuturasComSupervisorOuPregador(origem, pessoaId, HOJE, MES_ATUAL);
        verify(cultoRepository)
                .findByIgrejaIdAndPregadorIdAndDataGreaterThanEqualAndStatusNotIn(origem, pessoaId, HOJE, CULTO_ENCERRADO);
        verify(cronogramaEventoRepository)
                .findByIgrejaIdAndSupervisorPessoaIdAndDataEventoGreaterThanEqualAndStatusNotIn(origem, pessoaId, HOJE, EVENTO_ENCERRADO);
        verify(cronogramaConteudoRepository).findFuturosComPregador(origem, pessoaId, HOJE, EVENTO_ENCERRADO);
    }

    @Test
    void semNadaFuturoNaoAlteraNadaERetornaZero() {
        int ajustadas = service.ajustarNaSaida(pessoaId, origem);

        assertThat(ajustadas).isZero();
        verify(escalaVoluntarioRepository, never()).deleteAll(anyList());
        verify(escalaVoluntarioRepository, never()).saveAll(anyList());
        verify(escalaRepository, never()).saveAll(anyList());
        verify(cultoRepository, never()).saveAll(anyList());
        verify(cronogramaEventoRepository, never()).saveAll(anyList());
        verify(cronogramaConteudoRepository, never()).saveAll(anyList());
        verify(categoriaCultoRepository, never()).saveAll(anyList());
    }

    @Test
    void cultoFuturoComElaPregandoPassaParaOTitular() {
        Culto culto = new Culto();
        culto.setPregadorId(pessoaId);
        when(cultoRepository.findByIgrejaIdAndPregadorIdAndDataGreaterThanEqualAndStatusNotIn(origem, pessoaId, HOJE, CULTO_ENCERRADO))
                .thenReturn(List.of(culto));

        int ajustadas = service.ajustarNaSaida(pessoaId, origem);

        assertThat(culto.getPregadorId()).isEqualTo(titularId);
        verify(cultoRepository).saveAll(List.of(culto));
        assertThat(ajustadas).isEqualTo(1);
    }

    @Test
    void cronogramaFuturoTrocaSupervisorEPregadorPeloTitular() {
        CronogramaEvento evento = new CronogramaEvento();
        evento.setSupervisorPessoaId(pessoaId);
        CronogramaConteudo conteudo = new CronogramaConteudo();
        conteudo.setPregadorPessoaId(pessoaId);
        when(cronogramaEventoRepository
                .findByIgrejaIdAndSupervisorPessoaIdAndDataEventoGreaterThanEqualAndStatusNotIn(origem, pessoaId, HOJE, EVENTO_ENCERRADO))
                .thenReturn(List.of(evento));
        when(cronogramaConteudoRepository.findFuturosComPregador(origem, pessoaId, HOJE, EVENTO_ENCERRADO))
                .thenReturn(List.of(conteudo));

        int ajustadas = service.ajustarNaSaida(pessoaId, origem);

        assertThat(evento.getSupervisorPessoaId()).isEqualTo(titularId);
        assertThat(conteudo.getPregadorPessoaId()).isEqualTo(titularId);
        verify(cronogramaEventoRepository).saveAll(List.of(evento));
        verify(cronogramaConteudoRepository).saveAll(List.of(conteudo));
        assertThat(ajustadas).isEqualTo(2);
    }

    @Test
    void escalaFuturaTrocaSoOPapelQueElaOcupa() {
        Escala comoSupervisora = new Escala();
        comoSupervisora.setSupervisorPessoaId(pessoaId);
        comoSupervisora.setPregadorPessoaId(outraPessoa);
        Escala comoPregadora = new Escala();
        comoPregadora.setSupervisorPessoaId(outraPessoa);
        comoPregadora.setPregadorPessoaId(pessoaId);
        when(escalaRepository.findFuturasComSupervisorOuPregador(origem, pessoaId, HOJE, MES_ATUAL))
                .thenReturn(List.of(comoSupervisora, comoPregadora));

        int ajustadas = service.ajustarNaSaida(pessoaId, origem);

        assertThat(comoSupervisora.getSupervisorPessoaId()).isEqualTo(titularId);
        assertThat(comoSupervisora.getPregadorPessoaId()).isEqualTo(outraPessoa);
        assertThat(comoPregadora.getSupervisorPessoaId()).isEqualTo(outraPessoa);
        assertThat(comoPregadora.getPregadorPessoaId()).isEqualTo(titularId);
        verify(escalaRepository).saveAll(List.of(comoSupervisora, comoPregadora));
        assertThat(ajustadas).isEqualTo(2);
    }

    @Test
    void igrejaSemTitularDeixaOPapelVazio() {
        Igreja semTitular = new Igreja();
        semTitular.setId(origem);
        when(igrejaRepository.findById(origem)).thenReturn(Optional.of(semTitular));
        Culto culto = new Culto();
        culto.setPregadorId(pessoaId);
        when(cultoRepository.findByIgrejaIdAndPregadorIdAndDataGreaterThanEqualAndStatusNotIn(origem, pessoaId, HOJE, CULTO_ENCERRADO))
                .thenReturn(List.of(culto));

        service.ajustarNaSaida(pessoaId, origem);

        assertThat(culto.getPregadorId()).isNull();
    }

    @Test
    void contaTudoQueFoiAjustado() {
        when(escalaVoluntarioRepository.findFuturasDaPessoaNaIgreja(pessoaId, origem, HOJE, MES_ATUAL))
                .thenReturn(List.of(new EscalaVoluntario(), new EscalaVoluntario()));
        Escala escala = new Escala();
        escala.setSupervisorPessoaId(pessoaId);
        escala.setPregadorPessoaId(pessoaId);
        when(escalaRepository.findFuturasComSupervisorOuPregador(origem, pessoaId, HOJE, MES_ATUAL))
                .thenReturn(List.of(escala));
        Culto culto = new Culto();
        culto.setPregadorId(pessoaId);
        when(cultoRepository.findByIgrejaIdAndPregadorIdAndDataGreaterThanEqualAndStatusNotIn(origem, pessoaId, HOJE, CULTO_ENCERRADO))
                .thenReturn(List.of(culto));

        int ajustadas = service.ajustarNaSaida(pessoaId, origem);

        // 2 designações + 1 escala (supervisor e pregador na mesma escala contam uma vez) + 1 culto.
        assertThat(ajustadas).isEqualTo(4);
        assertThat(escala.getSupervisorPessoaId()).isEqualTo(titularId);
        assertThat(escala.getPregadorPessoaId()).isEqualTo(titularId);
    }

    @Test
    void consultaSoAIgrejaDeOrigem() {
        service.ajustarNaSaida(pessoaId, origem);

        verify(escalaVoluntarioRepository, never())
                .findFuturasDaPessoaNaIgreja(any(), org.mockito.ArgumentMatchers.argThat(id -> !origem.equals(id)), any(), any());
        verify(escalaRepository, never())
                .findFuturasComSupervisorOuPregador(org.mockito.ArgumentMatchers.argThat(id -> !origem.equals(id)), any(), any(), any());
        verify(cultoRepository, never())
                .findByIgrejaIdAndPregadorIdAndDataGreaterThanEqualAndStatusNotIn(org.mockito.ArgumentMatchers.argThat(id -> !origem.equals(id)), any(), any(), any());
        verify(categoriaCultoRepository, never())
                .findByIgrejaIdAndPregadorPadraoId(org.mockito.ArgumentMatchers.argThat(id -> !origem.equals(id)), any());
        verify(categoriaCultoRepository, never()).findAll();
        verify(cronogramaEventoRepository, never()).findAll();
        verify(cultoRepository, never()).findAll();
        verify(escalaRepository, never()).findAll();
        verify(escalaVoluntarioRepository, never()).findAll();
        verify(cronogramaConteudoRepository, never()).findAll();
        verify(cronogramaConteudoRepository).findFuturosComPregador(eq(origem), eq(pessoaId), eq(HOJE), eq(EVENTO_ENCERRADO));
    }

    // --- pregador padrão da categoria de culto (modelo dos próximos cultos) ---

    private CategoriaCulto categoriaComPregadorPadrao(UUID pregadorPadraoId) {
        CategoriaCulto categoria = new CategoriaCulto();
        categoria.setId(UUID.randomUUID());
        categoria.setIgrejaId(origem);
        categoria.setPregadorPadraoId(pregadorPadraoId);
        return categoria;
    }

    @Test
    void categoriaComElaComoPregadorPadraoPassaParaOTitular() {
        CategoriaCulto categoria = categoriaComPregadorPadrao(pessoaId);
        when(categoriaCultoRepository.findByIgrejaIdAndPregadorPadraoId(origem, pessoaId)).thenReturn(List.of(categoria));

        int ajustadas = service.ajustarNaSaida(pessoaId, origem);

        assertThat(categoria.getPregadorPadraoId()).isEqualTo(titularId);
        verify(categoriaCultoRepository).saveAll(List.of(categoria));
        assertThat(ajustadas).isEqualTo(1);
    }

    @Test
    void depoisDaLimpezaCultoSemPregadorNaoResolveMaisParaEla() {
        CategoriaCulto categoria = categoriaComPregadorPadrao(pessoaId);
        when(categoriaCultoRepository.findByIgrejaIdAndPregadorPadraoId(origem, pessoaId)).thenReturn(List.of(categoria));

        service.ajustarNaSaida(pessoaId, origem);

        // CultoService real: culto sem pregador cai no pregador padrão da categoria (resolverPregador).
        CultoRepository cultos = org.mockito.Mockito.mock(CultoRepository.class);
        CategoriaCultoRepository categorias = org.mockito.Mockito.mock(CategoriaCultoRepository.class);
        CultoService cultoService = new CultoService(cultos, categorias,
                org.mockito.Mockito.mock(CultoRelatorioRepository.class),
                org.mockito.Mockito.mock(AutorizacaoService.class),
                org.mockito.Mockito.mock(PessoaRepository.class));
        Culto semPregador = new Culto();
        semPregador.setId(UUID.randomUUID());
        semPregador.setIgrejaId(origem);
        semPregador.setCategoriaCultoId(categoria.getId());
        when(cultos.findById(semPregador.getId())).thenReturn(Optional.of(semPregador));
        when(categorias.findById(categoria.getId())).thenReturn(Optional.of(categoria));

        CultoConteudoResponse conteudo = cultoService.obterConteudo(semPregador.getId());

        assertThat(conteudo.pregadorId()).isEqualTo(titularId).isNotEqualTo(pessoaId);
    }

    @Test
    void designacaoSemDataDeEventoUsaADataDoEventoNaoACompetencia() throws Exception {
        Query query = EscalaVoluntarioRepository.class.getMethod("findFuturasDaPessoaNaIgreja",
                UUID.class, UUID.class, LocalDate.class, String.class).getAnnotation(Query.class);
        String jpql = query.value().replaceAll("\\s+", " ");

        assertThat(jpql).contains("LEFT JOIN CronogramaEvento ce ON ce.id = ev.eventoId");
        assertThat(jpql).contains("ev.dataItem IS NULL AND ce.id IS NOT NULL AND ce.dataEvento >= :hoje");
        assertThat(jpql).contains("ev.dataItem IS NULL AND ce.id IS NULL AND e.competencia >= :competencia");
    }

    // --- designações em escalas de SUPERVISOR/PREGADOR passam ao pastor titular ---

    private Escala escalaDoTipo(TipoEscala tipo) {
        Escala escala = new Escala();
        escala.setId(UUID.randomUUID());
        escala.setIgrejaId(origem);
        escala.setTipo(tipo);
        return escala;
    }

    private EscalaVoluntario designacaoDela(Escala escala, LocalDate data, UUID eventoId) {
        EscalaVoluntario ev = new EscalaVoluntario();
        ev.setId(UUID.randomUUID());
        ev.setEscalaId(escala.getId());
        ev.setPessoaId(pessoaId);
        ev.setDataItem(data);
        ev.setEventoId(eventoId);
        ev.setStatus(StatusEscalaVoluntario.CONFIRMADO);
        ev.setConfirmadoPeloVoluntario(true);
        ev.setObservacoes("Confirmo");
        return ev;
    }

    private void futuras(List<Escala> escalas, EscalaVoluntario... designacoes) {
        when(escalaVoluntarioRepository.findFuturasDaPessoaNaIgreja(pessoaId, origem, HOJE, MES_ATUAL))
                .thenReturn(List.of(designacoes));
        when(escalaRepository.findAllById(any())).thenReturn(escalas);
    }

    @Test
    void designacaoDeSupervisorPassaAoTitularPendente() {
        Escala supervisores = escalaDoTipo(TipoEscala.SUPERVISOR);
        UUID eventoId = UUID.randomUUID();
        EscalaVoluntario dela = designacaoDela(supervisores, HOJE.plusDays(7), eventoId);
        futuras(List.of(supervisores), dela);

        int ajustadas = service.ajustarNaSaida(pessoaId, origem);

        assertThat(dela.getPessoaId()).isEqualTo(titularId);
        assertThat(dela.getStatus()).isEqualTo(StatusEscalaVoluntario.PENDENTE);
        assertThat(dela.getConfirmadoPeloVoluntario()).isFalse();
        assertThat(dela.getObservacoes()).isNull();
        assertThat(dela.getDataItem()).isEqualTo(HOJE.plusDays(7));
        assertThat(dela.getEventoId()).isEqualTo(eventoId);
        assertThat(dela.getEscalaId()).isEqualTo(supervisores.getId());
        verify(escalaVoluntarioRepository).saveAll(List.of(dela));
        verify(escalaVoluntarioRepository, never()).deleteAll(anyList());
        assertThat(ajustadas).isEqualTo(1);
    }

    @Test
    void designacaoDePregadorSemTitularEhRemovida() {
        Igreja semTitular = new Igreja();
        semTitular.setId(origem);
        when(igrejaRepository.findById(origem)).thenReturn(Optional.of(semTitular));
        Escala pregadores = escalaDoTipo(TipoEscala.PREGADOR);
        EscalaVoluntario dela = designacaoDela(pregadores, HOJE, null);
        futuras(List.of(pregadores), dela);

        int ajustadas = service.ajustarNaSaida(pessoaId, origem);

        verify(escalaVoluntarioRepository).deleteAll(List.of(dela));
        verify(escalaVoluntarioRepository, never()).saveAll(anyList());
        assertThat(ajustadas).isEqualTo(1);
    }

    @Test
    void titularJaDesignadoNoMesmoDiaFazADesignacaoDelaSerRemovida() {
        Escala pregadores = escalaDoTipo(TipoEscala.PREGADOR);
        EscalaVoluntario dela = designacaoDela(pregadores, HOJE.plusDays(3), null);
        EscalaVoluntario doTitular = designacaoDela(pregadores, HOJE.plusDays(3), null);
        doTitular.setPessoaId(titularId);
        futuras(List.of(pregadores), dela);
        when(escalaVoluntarioRepository.findByEscalaIdInAndPessoaId(any(), eq(titularId))).thenReturn(List.of(doTitular));

        int ajustadas = service.ajustarNaSaida(pessoaId, origem);

        assertThat(dela.getPessoaId()).isEqualTo(pessoaId);
        verify(escalaVoluntarioRepository).deleteAll(List.of(dela));
        verify(escalaVoluntarioRepository, never()).saveAll(anyList());
        assertThat(ajustadas).isEqualTo(1);
    }

    @Test
    void titularJaDesignadoNoMesmoEventoSemDataFazADesignacaoDelaSerRemovida() {
        Escala supervisores = escalaDoTipo(TipoEscala.SUPERVISOR);
        UUID eventoId = UUID.randomUUID();
        EscalaVoluntario dela = designacaoDela(supervisores, null, eventoId);
        EscalaVoluntario doTitular = designacaoDela(supervisores, null, eventoId);
        doTitular.setPessoaId(titularId);
        futuras(List.of(supervisores), dela);
        when(escalaVoluntarioRepository.findByEscalaIdInAndPessoaId(any(), eq(titularId))).thenReturn(List.of(doTitular));

        service.ajustarNaSaida(pessoaId, origem);

        verify(escalaVoluntarioRepository).deleteAll(List.of(dela));
    }

    @Test
    void titularEmOutroDiaNaoImpedeAReatribuicao() {
        Escala supervisores = escalaDoTipo(TipoEscala.SUPERVISOR);
        EscalaVoluntario dela = designacaoDela(supervisores, HOJE.plusDays(1), null);
        EscalaVoluntario doTitular = designacaoDela(supervisores, HOJE.plusDays(8), null);
        doTitular.setPessoaId(titularId);
        futuras(List.of(supervisores), dela);
        when(escalaVoluntarioRepository.findByEscalaIdInAndPessoaId(any(), eq(titularId))).thenReturn(List.of(doTitular));

        service.ajustarNaSaida(pessoaId, origem);

        assertThat(dela.getPessoaId()).isEqualTo(titularId);
        verify(escalaVoluntarioRepository).saveAll(List.of(dela));
    }

    @Test
    void designacoesDeCultoEMinisterioContinuamVirandoVaga() {
        Escala culto = escalaDoTipo(TipoEscala.CULTO);
        Escala ministerio = escalaDoTipo(TipoEscala.MINISTERIO);
        Escala pregadores = escalaDoTipo(TipoEscala.PREGADOR);
        EscalaVoluntario noCulto = designacaoDela(culto, HOJE, null);
        EscalaVoluntario noMinisterio = designacaoDela(ministerio, HOJE, null);
        EscalaVoluntario pregando = designacaoDela(pregadores, HOJE, null);
        futuras(List.of(culto, ministerio, pregadores), noCulto, noMinisterio, pregando);

        int ajustadas = service.ajustarNaSaida(pessoaId, origem);

        verify(escalaVoluntarioRepository).deleteAll(List.of(noCulto, noMinisterio));
        verify(escalaVoluntarioRepository).saveAll(List.of(pregando));
        assertThat(noCulto.getPessoaId()).isEqualTo(pessoaId);
        assertThat(pregando.getPessoaId()).isEqualTo(titularId);
        assertThat(ajustadas).isEqualTo(3);
    }

    @Test
    void reatribuicaoSoOlhaEscalasDaIgrejaDeOrigem() {
        Escala supervisores = escalaDoTipo(TipoEscala.SUPERVISOR);
        EscalaVoluntario dela = designacaoDela(supervisores, HOJE, null);
        futuras(List.of(supervisores), dela);

        service.ajustarNaSaida(pessoaId, origem);

        // Só as escalas das designações achadas na origem são consultadas.
        verify(escalaRepository).findAllById(java.util.Set.of(supervisores.getId()));
        verify(escalaVoluntarioRepository).findByEscalaIdInAndPessoaId(java.util.Set.of(supervisores.getId()), titularId);
    }
}
