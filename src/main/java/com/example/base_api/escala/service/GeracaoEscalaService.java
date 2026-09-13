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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GeracaoEscalaService {

    private final EscalaRepository escalaRepository;
    private final EscalaServicoRepository escalaServicoRepository;
    private final EscalaVoluntarioRepository escalaVoluntarioRepository;
    private final CronogramaEventoRepository cronogramaEventoRepository;
    private final PessoaServicoRepository pessoaServicoRepository;
    private final PessoaRepository pessoaRepository;
    private final PessoaPausaRepository pessoaPausaRepository;
    private final DisponibilidadeSemanalRepository disponibilidadeSemanalRepository;
    private final IndisponibilidadeRepository indisponibilidadeRepository;
    private final ServicoService servicoService;
    private final MinisterioRepository ministerioRepository;
    private final AutorizacaoService autorizacaoService;

    public GeracaoEscalaService(
            EscalaRepository escalaRepository,
            EscalaServicoRepository escalaServicoRepository,
            EscalaVoluntarioRepository escalaVoluntarioRepository,
            CronogramaEventoRepository cronogramaEventoRepository,
            PessoaServicoRepository pessoaServicoRepository,
            PessoaRepository pessoaRepository,
            PessoaPausaRepository pessoaPausaRepository,
            DisponibilidadeSemanalRepository disponibilidadeSemanalRepository,
            IndisponibilidadeRepository indisponibilidadeRepository,
            ServicoService servicoService,
            MinisterioRepository ministerioRepository,
            AutorizacaoService autorizacaoService
    ) {
        this.escalaRepository = escalaRepository;
        this.escalaServicoRepository = escalaServicoRepository;
        this.escalaVoluntarioRepository = escalaVoluntarioRepository;
        this.cronogramaEventoRepository = cronogramaEventoRepository;
        this.pessoaServicoRepository = pessoaServicoRepository;
        this.pessoaRepository = pessoaRepository;
        this.pessoaPausaRepository = pessoaPausaRepository;
        this.disponibilidadeSemanalRepository = disponibilidadeSemanalRepository;
        this.indisponibilidadeRepository = indisponibilidadeRepository;
        this.servicoService = servicoService;
        this.ministerioRepository = ministerioRepository;
        this.autorizacaoService = autorizacaoService;
    }

    @Transactional
    public GeracaoEscalaResponse gerarSugestao(UUID escalaId) {
        Escala escala = buscar(escalaId);
        autorizacaoService.assertPodeMutarEscala(
                escala.getTipo(), escala.getIgrejaId(), escala.getCompetencia(), escala.getMinisterioId()
        );
        return gerar(escala);
    }

    @Transactional
    public List<CandidatoDisponibilidadeResponse> candidatos(UUID escalaId, LocalDate data, UUID servicoId) {
        Escala escala = buscar(escalaId);
        autorizacaoService.assertAcesso(escala.getIgrejaId());
        if (!autorizacaoService.podeVerEscala(escala)) {
            throw new AccessDeniedException("Sem permissão para ver esta escala");
        }

        List<EscalaServico> servicosDaEscala = escalaServicoRepository.findByEscalaId(escalaId);
        if (servicosDaEscala.isEmpty()) {
            return List.of();
        }
        List<EscalaServico> alvo = servicoId == null
                ? servicosDaEscala
                : servicosDaEscala.stream().filter(s -> s.getServicoId().equals(servicoId)).toList();
        if (alvo.isEmpty()) {
            throw new BusinessException("Este serviço não está previsto nesta escala");
        }

        List<UUID> servicoIds = alvo.stream().map(EscalaServico::getServicoId).toList();
        List<PessoaServico> vinculos = pessoaServicoRepository.findByServicoIdInAndAtivoTrue(servicoIds);
        Map<UUID, List<UUID>> porServico = new HashMap<>();
        Set<UUID> ids = new HashSet<>();
        for (PessoaServico v : vinculos) {
            porServico.computeIfAbsent(v.getServicoId(), k -> new ArrayList<>()).add(v.getPessoaId());
            ids.add(v.getPessoaId());
        }
        if (ids.isEmpty()) {
            return List.of();
        }

        List<UUID> idList = new ArrayList<>(ids);
        Map<UUID, Pessoa> pessoas = pessoaRepository.findAllById(idList).stream()
                .filter(p -> Boolean.TRUE.equals(p.getAtivo()))
                .collect(Collectors.toMap(Pessoa::getId, Function.identity()));

        Map<UUID, List<DisponibilidadeSemanal>> disponibilidades = agruparPorPessoa(
                disponibilidadeSemanalRepository.findByPessoaIdIn(idList));
        Map<UUID, List<Indisponibilidade>> indisponiveis = agruparPorPessoa(
                indisponibilidadeRepository.findByPessoaIdIn(idList));
        Map<UUID, List<PessoaPausa>> pausas = agruparPorPessoa(
                pessoaPausaRepository.findByPessoaIdInAndAtivaTrue(idList));

        Map<UUID, List<PessoaServico>> vinculosPorPessoa = agruparPorPessoa(
                pessoaServicoRepository.findByPessoaIdIn(idList));

        Map<UUID, List<EscalaVoluntario>> conflitos = Map.of();
        CronogramaEvento evento = null;
        if (data != null) {
            evento = cronogramaEventoRepository
                    .findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                            List.of(escala.getIgrejaId()), data, data)
                    .stream()
                    .filter(e -> Boolean.TRUE.equals(e.getAtivo()))
                    .findFirst()
                    .orElse(null);
            conflitos = agruparPorPessoa(
                    escalaVoluntarioRepository.findByPessoaIdInAndDataItemBetween(idList, data, data).stream()
                            .filter(i -> !escala.getId().equals(i.getEscalaId()))
                            .toList());
        }

        List<CandidatoDisponibilidadeResponse> resultado = new ArrayList<>();
        for (EscalaServico escalaServico : alvo) {
            List<UUID> candidatosServico = porServico.getOrDefault(escalaServico.getServicoId(), List.of());
            for (UUID pessoaId : candidatosServico) {
                Pessoa pessoa = pessoas.get(pessoaId);
                if (pessoa == null) {
                    continue;
                }
                List<String> servicos = vinculosPorPessoa.getOrDefault(pessoaId, List.of()).stream()
                        .filter(v -> Boolean.TRUE.equals(v.getAtivo()))
                        .map(v -> servicoService.buscarEntidade(v.getServicoId()).getNome())
                        .toList();
                List<CandidatoDisponibilidadeResponse.DiaDisponivel> dias = disponibilidades
                        .getOrDefault(pessoaId, List.of())
                        .stream()
                        .map(d -> new CandidatoDisponibilidadeResponse.DiaDisponivel(
                                d.getDiaSemana(), d.getHoraInicio(), d.getHoraFim(), d.getDisponivel()))
                        .toList();

                CandidatoDisponibilidadeResponse.AnaliseDia analise = null;
                if (evento != null) {
                    List<String> motivos = motivosIndisponibilidade(pessoaId, evento, indisponiveis, pausas, disponibilidades, conflitos);
                    List<CandidatoDisponibilidadeResponse.Conflito> conflitosDia = conflitos
                            .getOrDefault(pessoaId, List.of())
                            .stream()
                            .map(i -> toConflito(i))
                            .toList();
                    List<String> todosMotivos = new ArrayList<>(motivos);
                    if (!conflitosDia.isEmpty()) {
                        todosMotivos.removeIf(m -> m.startsWith("Conflito"));
                        conflitosDia.forEach(c -> todosMotivos.add(
                                "Conflito com " + c.escalaTitulo()
                                        + (c.ministerioNome() != null ? " (" + c.ministerioNome() + ")" : "")));
                    }
                    analise = new CandidatoDisponibilidadeResponse.AnaliseDia(
                            evento.getDataEvento(),
                            evento.getTitulo(),
                            motivos.isEmpty() && conflitosDia.isEmpty(),
                            todosMotivos,
                            conflitosDia
                    );
                }

                resultado.add(new CandidatoDisponibilidadeResponse(
                        escalaServico.getServicoId(),
                        servicoService.buscarEntidade(escalaServico.getServicoId()).getNome(),
                        pessoa.getId(),
                        pessoa.getNome(),
                        servicos,
                        dias,
                        analise
                ));
            }
        }
        return resultado;
    }

    @Transactional
    public GeracaoEscalaResponse gerar(Escala escala) {
        YearMonth competencia = YearMonth.parse(escala.getCompetencia());
        LocalDate inicio = competencia.atDay(1);
        LocalDate fim = competencia.atEndOfMonth();

        List<CronogramaEvento> eventos = cronogramaEventoRepository
                .findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                        List.of(escala.getIgrejaId()), inicio, fim)
                .stream()
                .filter(e -> Boolean.TRUE.equals(e.getAtivo()))
                .toList();
        if (eventos.isEmpty()) {
            return new GeracaoEscalaResponse(
                    escala.getId(),
                    "SEM_EVENTOS",
                    "Nenhum culto ou evento cadastrado no cronograma para " + competencia,
                    List.of()
            );
        }

        List<EscalaServico> escalaServicos = escalaServicoRepository.findByEscalaId(escala.getId());
        List<ServicoNecessidade> necessidades = escalaServicos.stream()
                .map(es -> new ServicoNecessidade(
                        es.getServicoId(),
                        servicoService.buscarEntidade(es.getServicoId()).getNome(),
                        es.getQuantidadeNecessaria()))
                .toList();

        boolean designacaoFixa = necessidades.isEmpty()
                && (escala.getTipo() == TipoEscala.SUPERVISOR || escala.getTipo() == TipoEscala.PREGADOR);
        UUID pessoaFixa = designacaoFixa
                ? (escala.getTipo() == TipoEscala.PREGADOR
                        ? escala.getPregadorPessoaId()
                        : escala.getSupervisorPessoaId())
                : null;

        if (necessidades.isEmpty() && !designacaoFixa) {
            return new GeracaoEscalaResponse(
                    escala.getId(),
                    "SEM_SERVICOS",
                    "Adicione serviços necessários à escala para gerar as sugestões.",
                    List.of()
            );
        }

        Set<UUID> candidatos = new HashSet<>();
        Map<UUID, List<UUID>> candidatosPorServico = new HashMap<>();
        if (!necessidades.isEmpty()) {
            for (PessoaServico vinculo : pessoaServicoRepository.findByServicoIdInAndAtivoTrue(
                    necessidades.stream().map(ServicoNecessidade::servicoId).toList())) {
                candidatos.add(vinculo.getPessoaId());
                candidatosPorServico.computeIfAbsent(vinculo.getServicoId(), k -> new ArrayList<>())
                        .add(vinculo.getPessoaId());
            }
        }
        if (pessoaFixa != null) {
            candidatos.add(pessoaFixa);
        }
        List<UUID> candidatoIds = new ArrayList<>(candidatos);

        Map<UUID, List<Indisponibilidade>> indisponiveis = agruparPorPessoa(
                indisponibilidadeRepository.findByPessoaIdIn(candidatoIds));
        Map<UUID, List<PessoaPausa>> pausas = agruparPorPessoa(
                pessoaPausaRepository.findByPessoaIdInAndAtivaTrue(candidatoIds));
        Map<UUID, List<DisponibilidadeSemanal>> disponibilidades = agruparPorPessoa(
                disponibilidadeSemanalRepository.findByPessoaIdIn(candidatoIds));
        Map<UUID, List<EscalaVoluntario>> conflitos = agruparPorPessoa(
                escalaVoluntarioRepository.findByPessoaIdInAndDataItemBetween(candidatoIds, inicio, fim).stream()
                        .filter(i -> !escala.getId().equals(i.getEscalaId()))
                        .toList());

        Map<UUID, Pessoa> pessoas = pessoaRepository.findAllById(candidatoIds).stream()
                .filter(p -> Boolean.TRUE.equals(p.getAtivo()))
                .collect(Collectors.toMap(Pessoa::getId, Function.identity()));

        List<EscalaVoluntario> existentes = escalaVoluntarioRepository.findByEscalaId(escala.getId());
        Map<UUID, Long> cargaPorPessoa = new HashMap<>();
        Map<UUID, Set<UUID>> pessoasPorEvento = new HashMap<>();
        Map<UUID, Map<UUID, Long>> ocupadosPorEvento = new HashMap<>();
        for (EscalaVoluntario ev : existentes) {
            cargaPorPessoa.merge(ev.getPessoaId(), 1L, Long::sum);
            if (ev.getEventoId() != null) {
                pessoasPorEvento.computeIfAbsent(ev.getEventoId(), k -> new HashSet<>()).add(ev.getPessoaId());
                if (ev.getServicoId() != null) {
                    ocupadosPorEvento.computeIfAbsent(ev.getEventoId(), k -> new HashMap<>())
                            .merge(ev.getServicoId(), 1L, Long::sum);
                }
            }
        }

        List<EscalaVoluntario> novosItens = new ArrayList<>();
        List<GeracaoEscalaResponse.SugestaoItem> sugestoes = new ArrayList<>();
        int totalVagas = 0;
        int preenchidas = 0;

        for (CronogramaEvento evento : eventos) {
            if (designacaoFixa) {
                totalVagas++;
                if (pessoaFixa != null && pessoas.containsKey(pessoaFixa)) {
                    Set<UUID> jaNoEvento = pessoasPorEvento.computeIfAbsent(evento.getId(), k -> new HashSet<>());
                    if (!jaNoEvento.contains(pessoaFixa)
                            && disponivel(pessoaFixa, evento, indisponiveis, pausas, disponibilidades, conflitos)) {
                        jaNoEvento.add(pessoaFixa);
                        novosItens.add(novoItem(escala, evento, pessoaFixa, null));
                        preenchidas++;
                        sugestoes.add(new GeracaoEscalaResponse.SugestaoItem(
                                null,
                                escala.getTipo() == TipoEscala.PREGADOR ? "Pregador" : "Supervisor",
                                pessoaFixa,
                                pessoas.get(pessoaFixa).getNome(),
                                evento.getDataEvento(),
                                evento.getId(),
                                "Disponível para o evento"
                        ));
                    }
                }
                continue;
            }

            for (ServicoNecessidade nece : necessidades) {
                totalVagas += nece.quantidade();
                long ocupados = ocupadosPorEvento.getOrDefault(evento.getId(), Map.of())
                        .getOrDefault(nece.servicoId(), 0L);
                Set<UUID> jaNoEvento = pessoasPorEvento.computeIfAbsent(evento.getId(), k -> new HashSet<>());
                List<UUID> pool = candidatosPorServico.getOrDefault(nece.servicoId(), List.of());
                for (long slot = ocupados; slot < nece.quantidade(); slot++) {
                    UUID escolhido = pool.stream()
                            .filter(p -> !jaNoEvento.contains(p))
                            .filter(p -> pessoas.containsKey(p))
                            .filter(p -> disponivel(p, evento, indisponiveis, pausas, disponibilidades, conflitos))
                            .min(Comparator
                                    .comparingLong((UUID p) -> cargaPorPessoa.getOrDefault(p, 0L))
                                    .thenComparing(p -> pessoas.get(p).getNome()))
                            .orElse(null);
                    if (escolhido == null) {
                        break;
                    }
                    jaNoEvento.add(escolhido);
                    cargaPorPessoa.merge(escolhido, 1L, Long::sum);
                    novosItens.add(novoItem(escala, evento, escolhido, nece.servicoId()));
                    preenchidas++;
                    sugestoes.add(new GeracaoEscalaResponse.SugestaoItem(
                            nece.servicoId(),
                            nece.servicoNome(),
                            escolhido,
                            pessoas.get(escolhido).getNome(),
                            evento.getDataEvento(),
                            evento.getId(),
                            "Disponível e de menor carga no mês"
                    ));
                }
            }
        }

        if (!novosItens.isEmpty()) {
            escalaVoluntarioRepository.saveAll(novosItens);
        }

        String status = preenchidas == 0
                ? "SEM_DISPONIVEL"
                : (preenchidas >= totalVagas ? "COMPLETA" : "PARCIAL");
        String message = "Sugestões geradas para " + escala.getTitulo() + ": " + preenchidas
                + " de " + totalVagas + " vagas preenchidas em " + eventos.size()
                + " culto(s)/evento(s) do mês.";
        return new GeracaoEscalaResponse(escala.getId(), status, message, sugestoes);
    }

    private Escala buscar(UUID id) {
        return escalaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escala não encontrada"));
    }

    private EscalaVoluntario novoItem(Escala escala, CronogramaEvento evento, UUID pessoaId, UUID servicoId) {
        EscalaVoluntario item = new EscalaVoluntario();
        item.setEscalaId(escala.getId());
        item.setPessoaId(pessoaId);
        item.setServicoId(servicoId);
        item.setDataItem(evento.getDataEvento());
        item.setEventoId(evento.getId());
        item.setHorario(evento.getHoraInicio());
        item.setStatus(StatusEscalaVoluntario.PENDENTE);
        item.setObservacoes("Sugestão automática");
        return item;
    }

    private boolean disponivel(
            UUID pessoaId,
            CronogramaEvento evento,
            Map<UUID, List<Indisponibilidade>> indisponiveis,
            Map<UUID, List<PessoaPausa>> pausas,
            Map<UUID, List<DisponibilidadeSemanal>> disponibilidades,
            Map<UUID, List<EscalaVoluntario>> conflitos
    ) {
        return motivosIndisponibilidade(pessoaId, evento, indisponiveis, pausas, disponibilidades, conflitos).isEmpty();
    }

    private List<String> motivosIndisponibilidade(
            UUID pessoaId,
            CronogramaEvento evento,
            Map<UUID, List<Indisponibilidade>> indisponiveis,
            Map<UUID, List<PessoaPausa>> pausas,
            Map<UUID, List<DisponibilidadeSemanal>> disponibilidades,
            Map<UUID, List<EscalaVoluntario>> conflitos
    ) {
        List<String> motivos = new ArrayList<>();
        LocalDate data = evento.getDataEvento();

        for (PessoaPausa p : pausas.getOrDefault(pessoaId, List.of())) {
            if (Boolean.TRUE.equals(p.getAtiva())
                    && !data.isBefore(p.getDataInicio())
                    && (p.getDataFim() == null || !data.isAfter(p.getDataFim()))) {
                motivos.add("Em pausa/afastamento neste período");
            }
        }

        for (Indisponibilidade i : indisponiveis.getOrDefault(pessoaId, List.of())) {
            if (!data.isBefore(i.getDataInicio())
                    && !data.isAfter(i.getDataFim())
                    && sobrepoe(evento.getHoraInicio(), evento.getHoraFim(), i.getHoraInicio(), i.getHoraFim())) {
                motivos.add("Indisponível no dia (" + i.getDataInicio() + " a " + i.getDataFim() + ")");
            }
        }

        List<DisponibilidadeSemanal> semana = disponibilidades.getOrDefault(pessoaId, List.of());
        if (!semana.isEmpty()) {
            DiaSemana dia = switch (data.getDayOfWeek()) {
                case SUNDAY -> DiaSemana.DOMINGO;
                case MONDAY -> DiaSemana.SEGUNDA;
                case TUESDAY -> DiaSemana.TERCA;
                case WEDNESDAY -> DiaSemana.QUARTA;
                case THURSDAY -> DiaSemana.QUINTA;
                case FRIDAY -> DiaSemana.SEXTA;
                case SATURDAY -> DiaSemana.SABADO;
            };
            boolean tem = semana.stream().anyMatch(d -> d.getDiaSemana() == dia
                    && Boolean.TRUE.equals(d.getDisponivel())
                    && dentroDoIntervalo(d.getHoraInicio(), d.getHoraFim(), evento.getHoraInicio(), evento.getHoraFim()));
            if (!tem) {
                motivos.add("Sem disponibilidade semanal no dia/horário");
            }
        }

        for (EscalaVoluntario outro : conflitos.getOrDefault(pessoaId, List.of())) {
            if (outro.getDataItem() == null || !outro.getDataItem().equals(data)) {
                continue;
            }
            LocalTime fimOutro = outro.getHorario();
            if (fimOutro != null && evento.getHoraFim() != null && evento.getHoraInicio() != null) {
                fimOutro = outro.getHorario().plus(Duration.between(evento.getHoraInicio(), evento.getHoraFim()));
            }
            if (sobrepoe(evento.getHoraInicio(), evento.getHoraFim(), outro.getHorario(), fimOutro)) {
                motivos.add("Conflito com outra escala no mesmo dia/horário");
            }
        }
        return motivos;
    }

    private CandidatoDisponibilidadeResponse.Conflito toConflito(EscalaVoluntario item) {
        Escala escala = escalaRepository.findById(item.getEscalaId()).orElse(null);
        String ministerioNome = null;
        if (escala != null && escala.getMinisterioId() != null) {
            ministerioNome = ministerioRepository.findById(escala.getMinisterioId())
                    .map(Ministerio::getNome)
                    .orElse(null);
        }
        String servicoNome = null;
        if (item.getServicoId() != null) {
            servicoNome = servicoService.buscarEntidade(item.getServicoId()).getNome();
        }
        return new CandidatoDisponibilidadeResponse.Conflito(
                item.getEscalaId(),
                escala != null ? escala.getTitulo() : null,
                ministerioNome,
                servicoNome,
                item.getDataItem(),
                item.getHorario()
        );
    }

    private static boolean dentroDoIntervalo(
            LocalTime disponivelInicio,
            LocalTime disponivelFim,
            LocalTime eventoInicio,
            LocalTime eventoFim
    ) {
        LocalTime fimEvento = eventoFim != null ? eventoFim : eventoInicio;
        return !disponivelInicio.isAfter(eventoInicio) && !disponivelFim.isBefore(fimEvento);
    }

    private static boolean sobrepoe(LocalTime aInicio, LocalTime aFim, LocalTime bInicio, LocalTime bFim) {
        if (bInicio == null && bFim == null) {
            return true;
        }
        if (bInicio == null) {
            return aInicio.isBefore(bFim);
        }
        if (bFim == null) {
            return aFim != null && aFim.isAfter(bInicio);
        }
        return aInicio.isBefore(bFim) && bInicio.isBefore(aFim != null ? aFim : aInicio);
    }

    private static <T extends com.example.base_api.common.entity.AuditableEntity> Map<UUID, List<T>> agruparPorPessoa(
            List<T> lista
    ) {
        return lista.stream().collect(Collectors.groupingBy(item -> {
            if (item instanceof DisponibilidadeSemanal d) {
                return d.getPessoaId();
            }
            if (item instanceof Indisponibilidade i) {
                return i.getPessoaId();
            }
            if (item instanceof PessoaPausa p) {
                return p.getPessoaId();
            }
            if (item instanceof EscalaVoluntario e) {
                return e.getPessoaId();
            }
            if (item instanceof PessoaServico v) {
                return v.getPessoaId();
            }
            throw new IllegalArgumentException("Tipo não mapeado: " + item.getClass());
        }));
    }

    private record ServicoNecessidade(UUID servicoId, String servicoNome, int quantidade) {
    }
}