package com.example.base_api.escala.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.common.util.Horarios;
import com.example.base_api.cronograma.entity.CronogramaEvento;
import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import com.example.base_api.cronograma.service.CronogramaService;
import com.example.base_api.culto.entity.Culto;
import com.example.base_api.culto.repository.CultoRepository;
import com.example.base_api.escala.dto.EscalaCreateRequest;
import com.example.base_api.escala.dto.EscalaResponse;
import com.example.base_api.escala.dto.EscalaServicoUpsertRequest;
import com.example.base_api.escala.dto.EscalaStatusRequest;
import com.example.base_api.escala.dto.EscalaUpdateRequest;
import com.example.base_api.escala.dto.EscalaVoluntarioCreateRequest;
import com.example.base_api.escala.dto.EscalaVoluntarioRespostaRequest;
import com.example.base_api.escala.dto.EscalaVoluntarioStatusRequest;
import com.example.base_api.escala.dto.EscalaVoluntarioUpdateRequest;
import com.example.base_api.escala.dto.VisaoMensalEscalaResponse;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.EscalaServico;
import com.example.base_api.escala.entity.EscalaVoluntario;
import com.example.base_api.escala.entity.StatusEscala;
import com.example.base_api.escala.entity.StatusEscalaVoluntario;
import com.example.base_api.escala.entity.TipoEscala;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaServicoRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.service.IgrejaContextoService;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.service.ServicoService;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EscalaService {

    private final EscalaRepository escalaRepository;
    private final EscalaServicoRepository escalaServicoRepository;
    private final EscalaVoluntarioRepository escalaVoluntarioRepository;
    private final ServicoService servicoService;
    private final PessoaService pessoaService;
    private final PessoaServicoRepository pessoaServicoRepository;
    private final CronogramaService cronogramaService;
    private final CronogramaEventoRepository cronogramaEventoRepository;
    private final CultoRepository cultoRepository;
    private final IgrejaContextoService igrejaContextoService;
    private final AutorizacaoService autorizacaoService;
    private final MinisterioRepository ministerioRepository;
    private final GeracaoEscalaService geracaoEscalaService;

    public EscalaService(
            EscalaRepository escalaRepository,
            EscalaServicoRepository escalaServicoRepository,
            EscalaVoluntarioRepository escalaVoluntarioRepository,
            ServicoService servicoService,
            PessoaService pessoaService,
            PessoaServicoRepository pessoaServicoRepository,
            CronogramaService cronogramaService,
            CronogramaEventoRepository cronogramaEventoRepository,
            CultoRepository cultoRepository,
            IgrejaContextoService igrejaContextoService,
            AutorizacaoService autorizacaoService,
            MinisterioRepository ministerioRepository,
            GeracaoEscalaService geracaoEscalaService
    ) {
        this.escalaRepository = escalaRepository;
        this.escalaServicoRepository = escalaServicoRepository;
        this.escalaVoluntarioRepository = escalaVoluntarioRepository;
        this.servicoService = servicoService;
        this.pessoaService = pessoaService;
        this.pessoaServicoRepository = pessoaServicoRepository;
        this.cronogramaService = cronogramaService;
        this.cronogramaEventoRepository = cronogramaEventoRepository;
        this.cultoRepository = cultoRepository;
        this.igrejaContextoService = igrejaContextoService;
        this.autorizacaoService = autorizacaoService;
        this.ministerioRepository = ministerioRepository;
        this.geracaoEscalaService = geracaoEscalaService;
    }

    @Transactional(readOnly = true)
    public List<EscalaResponse> listar(UUID igrejaId, String competencia, UUID pessoaId) {
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId);
        if (ids.isEmpty()) {
            return List.of();
        }

        Usuario usuario = autorizacaoService.requireUsuario();
        UUID pessoaFiltro = pessoaId;
        if (pessoaFiltro != null
                && !pessoaFiltro.equals(usuario.getPessoaId())
                && ids.stream().noneMatch(autorizacaoService::isPastorOuAdmin)) {
            throw new AccessDeniedException("Sem permissão para ver escalas de outra pessoa");
        }

        List<Escala> escalas;
        if (competencia != null && !competencia.isBlank()) {
            escalas = escalaRepository.findByIgrejaIdInAndCompetenciaOrderByTipoAscTituloAsc(ids, competencia);
        } else {
            escalas = escalaRepository.findByIgrejaIdInOrderByCompetenciaDescTituloAsc(ids);
        }
        escalas = escalas.stream().filter(autorizacaoService::podeVerEscala).toList();
        if (pessoaFiltro != null) {
            List<UUID> escalaIdsDaPessoa = escalaRepository.findByIgrejaIdInAndPessoaDesignada(ids, pessoaFiltro)
                    .stream()
                    .map(Escala::getId)
                    .toList();
            escalas = escalas.stream().filter(e -> escalaIdsDaPessoa.contains(e.getId())).toList();
        }
        return toResponses(escalas, pessoaFiltro);
    }

    @Transactional(readOnly = true)
    public EscalaResponse buscarPorId(UUID id) {
        Escala escala = buscarEntidade(id);
        autorizacaoService.assertAcesso(escala.getIgrejaId());
        if (!autorizacaoService.podeVerEscala(escala)) {
            throw new AccessDeniedException("Sem permissão para ver esta escala");
        }
        return toResponse(escala, null);
    }

    @Transactional(readOnly = true)
    public List<VisaoMensalEscalaResponse> visaoMensal(UUID igrejaId, String competencia, UUID ministerioId) {
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId);
        if (ids.isEmpty()) {
            return List.of();
        }
        String competenciaValida = competencia == null || competencia.isBlank()
                ? YearMonth.now().toString()
                : validarCompetencia(competencia);
        YearMonth mes = YearMonth.parse(competenciaValida);
        LocalDate inicio = mes.atDay(1);
        LocalDate fim = mes.atEndOfMonth();

        UUID pessoaLogada = autorizacaoService.requireUsuario().getPessoaId();
        List<Ministerio> visiveis = ministerioRepository.findByIgrejaIdInOrderByNomeAsc(ids);

        List<Ministerio> selecionados;
        if (ministerioId != null) {
            Ministerio alvo = visiveis.stream()
                    .filter(m -> m.getId().equals(ministerioId))
                    .findFirst()
                    .orElseThrow(() -> new AccessDeniedException("Ministério não encontrado ou fora do seu acesso"));
            if (!autorizacaoService.isPastorOuAdmin(alvo.getIgrejaId())
                    && (pessoaLogada == null || (!pessoaLogada.equals(alvo.getLiderPessoaId())
                            && !pessoaLogada.equals(alvo.getCoordenadorPessoaId())))) {
                throw new AccessDeniedException("Sem permissão para ver a escala deste ministério");
            }
            selecionados = List.of(alvo);
        } else {
            selecionados = visiveis.stream()
                    .filter(m -> autorizacaoService.isPastorOuAdmin(m.getIgrejaId())
                            || (pessoaLogada != null && (pessoaLogada.equals(m.getLiderPessoaId())
                                    || pessoaLogada.equals(m.getCoordenadorPessoaId()))))
                    .toList();
            if (selecionados.isEmpty()) {
                throw new AccessDeniedException(
                        "Sem permissão: informe um ministerioId de um ministério que você lidera"
                );
            }
        }

        return selecionados.stream()
                .map(m -> montarVisaoMensal(m, ids, competenciaValida, inicio, fim))
                .toList();
    }

    private VisaoMensalEscalaResponse montarVisaoMensal(
            Ministerio ministerio,
            List<UUID> igrejaIds,
            String competencia,
            LocalDate inicio,
            LocalDate fim
    ) {
        List<Escala> escalasDoMes = escalaRepository
                .findByMinisterioIdAndCompetenciaAndAtivoTrueOrderByDataEventoAsc(ministerio.getId(), competencia);
        Escala mensal = escalasDoMes.stream()
                .filter(e -> e.getTipo() == TipoEscala.MINISTERIO && e.getEventoId() == null)
                .findFirst()
                .orElse(null);

        Map<UUID, Integer> qtdPorServico = new HashMap<>();
        if (mensal != null) {
            for (EscalaServico necessidade : escalaServicoRepository.findByEscalaId(mensal.getId())) {
                qtdPorServico.put(necessidade.getServicoId(), necessidade.getQuantidadeNecessaria());
            }
        }

        List<EscalaVoluntario> designacoes = mensal == null
                ? List.of()
                : escalaVoluntarioRepository.findByEscalaId(mensal.getId());

        Map<UUID, List<EscalaVoluntario>> porEvento = designacoes.stream()
                .filter(d -> d.getEventoId() != null)
                .collect(Collectors.groupingBy(EscalaVoluntario::getEventoId));

        Map<LocalDate, List<EscalaVoluntario>> porData = designacoes.stream()
                .filter(d -> d.getDataItem() != null)
                .collect(Collectors.groupingBy(EscalaVoluntario::getDataItem));

        Set<UUID> servicoIdsDoMes = new HashSet<>(qtdPorServico.keySet());
        designacoes.forEach(d -> servicoIdsDoMes.add(d.getServicoId()));
        Nomes nomes = nomes(designacoes.stream().map(EscalaVoluntario::getPessoaId).toList(), servicoIdsDoMes);

        List<VisaoMensalEscalaResponse.ItemVisao> itens = new ArrayList<>();

        for (Culto culto : cultoRepository.filtrar(igrejaIds, null, competencia, null, inicio, fim, true)) {
            List<EscalaVoluntario> doCulto = porData.getOrDefault(culto.getData(), List.of()).stream()
                    .filter(d -> d.getEventoId() == null || culto.getHoraInicio().equals(d.getHorario()))
                    .toList();
            itens.add(new VisaoMensalEscalaResponse.ItemVisao(
                    "CULTO",
                    culto.getId(),
                    culto.getTitulo(),
                    culto.getData(),
                    culto.getHoraInicio(),
                    culto.getHoraFim(),
                    null,
                    montarServicos(qtdPorServico, doCulto, nomes)
            ));
        }

        for (CronogramaEvento evento : cronogramaEventoRepository
                .findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(igrejaIds, inicio, fim)) {
            if (!Boolean.TRUE.equals(evento.getAtivo())) {
                continue;
            }
            itens.add(new VisaoMensalEscalaResponse.ItemVisao(
                    "EVENTO",
                    evento.getId(),
                    evento.getTitulo(),
                    evento.getDataEvento(),
                    evento.getHoraInicio(),
                    evento.getHoraFim(),
                    evento.getLocalEvento(),
                    montarServicos(qtdPorServico, porEvento.getOrDefault(evento.getId(), List.of()), nomes)
            ));
        }

        itens.sort(Comparator.comparing(VisaoMensalEscalaResponse.ItemVisao::data)
                .thenComparing(Comparator.comparing(VisaoMensalEscalaResponse.ItemVisao::horaInicio,
                        Comparator.nullsLast(LocalTime::compareTo)))
                .thenComparing(VisaoMensalEscalaResponse.ItemVisao::origem));

        return new VisaoMensalEscalaResponse(
                competencia,
                ministerio.getIgrejaId(),
                ministerio.getId(),
                ministerio.getNome(),
                mensal != null ? mensal.getId() : null,
                mensal != null ? mensal.getTitulo() : null,
                mensal != null ? mensal.getStatus() : null,
                itens
        );
    }

    private List<VisaoMensalEscalaResponse.ServicoVisao> montarServicos(
            Map<UUID, Integer> qtdPorServico,
            List<EscalaVoluntario> designacoes,
            Nomes nomes
    ) {
        Map<UUID, List<EscalaVoluntario>> porServico = designacoes.stream()
                .filter(d -> d.getServicoId() != null)
                .collect(Collectors.groupingBy(EscalaVoluntario::getServicoId));

        Set<UUID> servicoIds = new LinkedHashSet<>(qtdPorServico.keySet());
        servicoIds.addAll(porServico.keySet());

        List<VisaoMensalEscalaResponse.ServicoVisao> resultado = new ArrayList<>();
        for (UUID servicoId : servicoIds.stream()
                .sorted(Comparator.comparing(nomes::servico, Comparator.nullsLast(String::compareTo)))
                .toList()) {
            Map<UUID, EscalaVoluntario> unicos = new LinkedHashMap<>();
            for (EscalaVoluntario d : porServico.getOrDefault(servicoId, List.of())) {
                unicos.putIfAbsent(d.getPessoaId(), d);
            }
            List<EscalaVoluntario> designados = new ArrayList<>(unicos.values());
            designados.sort(Comparator.comparing(d -> nomes.pessoa(d.getPessoaId()),
                    Comparator.nullsLast(String::compareTo)));
            Integer qtd = qtdPorServico.get(servicoId);
            int quantidade = qtd != null ? qtd : designados.size();
            resultado.add(new VisaoMensalEscalaResponse.ServicoVisao(
                    servicoId,
                    nomes.servico(servicoId),
                    quantidade,
                    designados.stream().map(d -> toDesignadoVisao(d, nomes)).toList()
            ));
        }
        return resultado;
    }

    private VisaoMensalEscalaResponse.DesignadoVisao toDesignadoVisao(EscalaVoluntario designacao, Nomes nomes) {
        return new VisaoMensalEscalaResponse.DesignadoVisao(
                designacao.getId(),
                designacao.getPessoaId(),
                nomes.pessoa(designacao.getPessoaId()),
                designacao.getStatus(),
                designacao.getConfirmadoPeloVoluntario(),
                designacao.getObservacoes()
        );
    }

    private String servicoNome(UUID servicoId) {
        if (servicoId == null) {
            return null;
        }
        return servicoService.buscarEntidade(servicoId).getNome();
    }

    @Transactional
    public EscalaResponse criar(EscalaCreateRequest request) {
        autorizacaoService.assertAcesso(request.igrejaId());
        String competencia = validarCompetencia(request.competencia());
        TipoEscala tipo = request.tipo();
        validarJanelaAbertura(competencia);
        autorizacaoService.assertPodeCriarEscala(tipo, request.igrejaId(), competencia, request.ministerioId());

        if (tipo == TipoEscala.MINISTERIO && request.ministerioId() == null) {
            throw new BusinessException("ministerioId é obrigatório para escala de ministério");
        }
        if (request.ministerioId() != null) {
            garantirMinisterioDaIgreja(request.ministerioId(), request.igrejaId());
        }

        Horarios.validarFimAposInicio(request.horaInicio(), request.horaFim());
        validarEvento(request.eventoId());

        Igreja igreja = igrejaContextoService.buscarIgreja(request.igrejaId());
        UUID supervisorId = request.supervisorPessoaId() != null
                ? request.supervisorPessoaId()
                : igreja.getPastorPessoaId();
        UUID pregadorId = request.pregadorPessoaId() != null
                ? request.pregadorPessoaId()
                : igreja.getPastorPessoaId();
        garantirPessoaDaIgreja(supervisorId, request.igrejaId());
        garantirPessoaDaIgreja(pregadorId, request.igrejaId());

        LocalDate dataEvento = request.dataEvento() != null
                ? request.dataEvento()
                : YearMonth.parse(competencia).atDay(1);
        LocalTime horaInicio = request.horaInicio() != null ? request.horaInicio() : LocalTime.MIDNIGHT;

        Escala escala = new Escala();
        escala.setTitulo(request.titulo().trim());
        escala.setDescricao(request.descricao());
        escala.setDataEvento(dataEvento);
        escala.setHoraInicio(horaInicio);
        escala.setHoraFim(request.horaFim());
        escala.setLocalEvento(request.localEvento());
        escala.setEventoId(request.eventoId());
        escala.setIgrejaId(request.igrejaId());
        escala.setCompetencia(competencia);
        escala.setTipo(tipo);
        escala.setMinisterioId(request.ministerioId());
        escala.setSupervisorPessoaId(supervisorId);
        escala.setPregadorPessoaId(pregadorId);
        escala.setAtivo(true);
        escala.setStatus(StatusEscala.EDITANDO);

        Escala salva = escalaRepository.save(escala);

        if (request.servicos() != null) {
            for (EscalaCreateRequest.EscalaServicoRequest item : request.servicos()) {
                garantirServicoDaIgreja(item.servicoId(), salva.getIgrejaId());
                EscalaServico escalaServico = new EscalaServico();
                escalaServico.setEscalaId(salva.getId());
                escalaServico.setServicoId(item.servicoId());
                escalaServico.setQuantidadeNecessaria(item.quantidadeNecessaria());
                escalaServicoRepository.save(escalaServico);
            }
        }

        if (request.itens() != null) {
            for (EscalaCreateRequest.EscalaItemRequest item : request.itens()) {
                persistirItem(salva, item);
            }
        }

        geracaoEscalaService.gerar(salva);

        return toResponse(salva, null);
    }

    @Transactional
    public EscalaResponse atualizar(UUID id, EscalaUpdateRequest request) {
        Escala escala = buscarEntidade(id);
        // Autoriza a mutação no estado atual da escala.
        autorizacaoService.assertPodeMutarEscala(
                escala.getTipo(), escala.getIgrejaId(), escala.getCompetencia(), escala.getMinisterioId()
        );

        // Calcula o estado resultante e reautoriza/valida, para que mudanças de tipo/ministério/competência
        // não escapem das regras de autorização nem cruzem tenants.
        TipoEscala novoTipo = request.tipo() != null ? request.tipo() : escala.getTipo();
        UUID novoMinisterioId = request.ministerioId() != null ? request.ministerioId() : escala.getMinisterioId();
        String novaCompetencia = request.competencia() != null && !request.competencia().isBlank()
                ? validarCompetencia(request.competencia())
                : escala.getCompetencia();
        if (request.competencia() != null && !request.competencia().isBlank()) {
            validarJanelaAbertura(novaCompetencia);
        }

        if (novoTipo == TipoEscala.MINISTERIO && novoMinisterioId == null) {
            throw new BusinessException("ministerioId é obrigatório para escala de ministério");
        }
        if (request.ministerioId() != null) {
            garantirMinisterioDaIgreja(request.ministerioId(), escala.getIgrejaId());
        }
        autorizacaoService.assertPodeMutarEscala(
                novoTipo, escala.getIgrejaId(), novaCompetencia, novoMinisterioId
        );

        Horarios.validarFimAposInicio(request.horaInicio(), request.horaFim());
        validarEvento(request.eventoId());

        escala.setTitulo(request.titulo().trim());
        escala.setDescricao(request.descricao());
        escala.setCompetencia(novaCompetencia);
        if (request.tipo() != null) {
            escala.setTipo(request.tipo());
        }
        if (request.ministerioId() != null) {
            escala.setMinisterioId(request.ministerioId());
        }
        if (request.supervisorPessoaId() != null) {
            garantirPessoaDaIgreja(request.supervisorPessoaId(), escala.getIgrejaId());
            escala.setSupervisorPessoaId(request.supervisorPessoaId());
        }
        if (request.pregadorPessoaId() != null) {
            garantirPessoaDaIgreja(request.pregadorPessoaId(), escala.getIgrejaId());
            escala.setPregadorPessoaId(request.pregadorPessoaId());
        }
        if (request.dataEvento() != null) {
            escala.setDataEvento(request.dataEvento());
        }
        if (request.horaInicio() != null) {
            escala.setHoraInicio(request.horaInicio());
        }
        escala.setHoraFim(request.horaFim());
        escala.setLocalEvento(request.localEvento());
        escala.setEventoId(request.eventoId());
        escala.setAtivo(request.ativo());

        return toResponse(escalaRepository.save(escala), null);
    }

    @Transactional
    public EscalaResponse atualizarStatus(UUID id, EscalaStatusRequest request) {
        Escala escala = buscarEntidade(id);
        assertPodeMutar(escala);
        StatusEscala atual = escala.getStatus();
        StatusEscala novo = request.status();
        if (atual != novo && !transicoesPermitidas().getOrDefault(atual, Set.of()).contains(novo)) {
            throw new BusinessException(
                    "Transição de status inválida: " + atual + " -> " + novo
            );
        }
        escala.setStatus(novo);
        Instant agora = Instant.now();
        if (novo == StatusEscala.EM_ANDAMENTO && escala.getDataLancamento() == null) {
            escala.setDataLancamento(agora);
        }
        if (novo == StatusEscala.FINALIZADA && escala.getDataFinalizacao() == null) {
            escala.setDataFinalizacao(agora);
        }
        if (novo != StatusEscala.EM_ANDAMENTO) {
            escala.setDataLancamento(null);
        }
        if (novo != StatusEscala.FINALIZADA && escala.getDataFinalizacao() != null) {
            escala.setDataFinalizacao(null);
        }
        return toResponse(escalaRepository.save(escala), null);
    }

    private static Map<StatusEscala, Set<StatusEscala>> transicoesPermitidas() {
        Map<StatusEscala, Set<StatusEscala>> mapa = new EnumMap<>(StatusEscala.class);
        mapa.put(StatusEscala.SUGERIDA, Set.of(StatusEscala.EDITANDO, StatusEscala.EM_ANDAMENTO));
        mapa.put(StatusEscala.EDITANDO, Set.of(StatusEscala.SUGERIDA, StatusEscala.EM_ANDAMENTO));
        mapa.put(StatusEscala.EM_ANDAMENTO, Set.of(StatusEscala.EDITANDO, StatusEscala.FINALIZADA));
        mapa.put(StatusEscala.FINALIZADA, Set.of(StatusEscala.EM_ANDAMENTO));
        return mapa;
    }

    @Transactional
    public EscalaResponse.EscalaServicoResponse adicionarServico(UUID escalaId, EscalaServicoUpsertRequest request) {
        Escala escala = buscarEntidade(escalaId);
        assertPodeMutar(escala);
        garantirServicoDaIgreja(request.servicoId(), escala.getIgrejaId());

        escalaServicoRepository.findByEscalaIdAndServicoId(escala.getId(), request.servicoId())
                .ifPresent(item -> {
                    throw new BusinessException("Este serviço já está na escala");
                });

        EscalaServico escalaServico = new EscalaServico();
        escalaServico.setEscalaId(escala.getId());
        escalaServico.setServicoId(request.servicoId());
        escalaServico.setQuantidadeNecessaria(request.quantidadeNecessaria());

        EscalaServico salvo = escalaServicoRepository.save(escalaServico);
        Servico servico = servicoService.buscarEntidade(salvo.getServicoId());
        return new EscalaResponse.EscalaServicoResponse(
                salvo.getId(),
                salvo.getServicoId(),
                servico.getNome(),
                salvo.getQuantidadeNecessaria()
        );
    }

    @Transactional
    public EscalaResponse.EscalaServicoResponse atualizarServico(
            UUID escalaId,
            UUID escalaServicoId,
            EscalaServicoUpsertRequest request
    ) {
        Escala escalaRef = buscarEntidade(escalaId);
        assertPodeMutar(escalaRef);
        EscalaServico escalaServico = escalaServicoRepository.findById(escalaServicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço da escala não encontrado"));

        if (!escalaServico.getEscalaId().equals(escalaId)) {
            throw new BusinessException("O serviço não pertence a esta escala");
        }

        garantirServicoDaIgreja(request.servicoId(), escalaRef.getIgrejaId());

        if (!escalaServico.getServicoId().equals(request.servicoId())) {
            escalaServicoRepository.findByEscalaIdAndServicoId(escalaId, request.servicoId())
                    .ifPresent(item -> {
                        throw new BusinessException("Este serviço já está na escala");
                    });
        }

        escalaServico.setServicoId(request.servicoId());
        escalaServico.setQuantidadeNecessaria(request.quantidadeNecessaria());

        EscalaServico salvo = escalaServicoRepository.save(escalaServico);
        Servico servico = servicoService.buscarEntidade(salvo.getServicoId());
        return new EscalaResponse.EscalaServicoResponse(
                salvo.getId(),
                salvo.getServicoId(),
                servico.getNome(),
                salvo.getQuantidadeNecessaria()
        );
    }

    @Transactional
    public void removerServico(UUID escalaId, UUID escalaServicoId) {
        Escala escalaRef = buscarEntidade(escalaId);
        assertPodeMutar(escalaRef);
        EscalaServico escalaServico = escalaServicoRepository.findById(escalaServicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço da escala não encontrado"));

        if (!escalaServico.getEscalaId().equals(escalaId)) {
            throw new BusinessException("O serviço não pertence a esta escala");
        }

        escalaServicoRepository.delete(escalaServico);
    }

    @Transactional
    public EscalaResponse.EscalaVoluntarioResponse designarVoluntario(
            UUID escalaId,
            EscalaVoluntarioCreateRequest request
    ) {
        Escala escala = buscarEntidade(escalaId);
        assertPodeMutar(escala);
        Pessoa pessoa = pessoaService.buscarEntidade(request.pessoaId());
        autorizacaoService.assertPessoaDaIgreja(pessoa, escala.getIgrejaId());
        Servico servico = servicoService.buscarEntidade(request.servicoId());

        EscalaServico necessidade = escalaServicoRepository
                .findByEscalaIdAndServicoId(escala.getId(), servico.getId())
                .orElseThrow(() -> new BusinessException("Este serviço não está previsto nesta escala"));

        if (pessoaServicoRepository.findByPessoaIdAndServicoId(pessoa.getId(), servico.getId())
                .filter(v -> Boolean.TRUE.equals(v.getAtivo()))
                .isEmpty()) {
            throw new BusinessException("A pessoa não está habilitada para este serviço");
        }

        LocalDate dataItem = request.data();
        UUID eventoId = request.eventoId();
        LocalTime horario = request.horario();
        if (eventoId != null) {
            CronogramaEvento evento = cronogramaService.buscarEntidade(eventoId);
            if (!evento.getIgrejaId().equals(escala.getIgrejaId())) {
                throw new BusinessException("O evento não pertence a esta igreja");
            }
            if (dataItem == null) {
                dataItem = evento.getDataEvento();
            }
            if (horario == null) {
                horario = evento.getHoraInicio();
            }
        }

        if (dataItem != null) {
            escalaVoluntarioRepository
                    .findByEscalaIdAndPessoaIdAndServicoIdAndDataItem(
                            escala.getId(), pessoa.getId(), servico.getId(), dataItem)
                    .ifPresent(v -> {
                        throw new BusinessException("A pessoa já está designada para este serviço nesta data e escala");
                    });
        } else {
            if (!escalaVoluntarioRepository
                    .findByEscalaIdAndPessoaIdAndServicoId(escala.getId(), pessoa.getId(), servico.getId())
                    .isEmpty()) {
                throw new BusinessException("A pessoa já está designada para este serviço nesta escala");
            }
        }

        long ocupados;
        if (dataItem != null) {
            ocupados = escalaVoluntarioRepository.countByEscalaIdAndServicoIdAndDataItemAndStatusIn(
                    escala.getId(),
                    servico.getId(),
                    dataItem,
                    List.of(StatusEscalaVoluntario.PENDENTE, StatusEscalaVoluntario.CONFIRMADO)
            );
        } else {
            ocupados = escalaVoluntarioRepository.countByEscalaIdAndServicoIdAndStatusIn(
                    escala.getId(),
                    servico.getId(),
                    List.of(StatusEscalaVoluntario.PENDENTE, StatusEscalaVoluntario.CONFIRMADO)
            );
        }
        if (ocupados >= necessidade.getQuantidadeNecessaria()) {
            throw new BusinessException("Quantidade necessária para este serviço já foi preenchida");
        }

        EscalaVoluntario designacao = new EscalaVoluntario();
        designacao.setEscalaId(escala.getId());
        designacao.setPessoaId(pessoa.getId());
        designacao.setServicoId(servico.getId());
        designacao.setDataItem(dataItem);
        designacao.setEventoId(eventoId);
        designacao.setHorario(horario);
        designacao.setStatus(StatusEscalaVoluntario.PENDENTE);
        designacao.setObservacoes(request.observacoes());

        return toVoluntarioResponse(escalaVoluntarioRepository.save(designacao));
    }

    @Transactional
    public EscalaResponse.EscalaVoluntarioResponse atualizarStatus(
            UUID escalaId,
            UUID designacaoId,
            EscalaVoluntarioStatusRequest request
    ) {
        Escala escalaRef = buscarEntidade(escalaId);
        assertPodeMutar(escalaRef);
        EscalaVoluntario designacao = escalaVoluntarioRepository.findById(designacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Designação não encontrada"));

        if (!designacao.getEscalaId().equals(escalaId)) {
            throw new BusinessException("A designação não pertence a esta escala");
        }

        designacao.setStatus(request.status());
        if (request.status() == StatusEscalaVoluntario.CONFIRMADO) {
            designacao.setConfirmadoPeloVoluntario(Boolean.TRUE.equals(request.confirmadoPeloVoluntario()));
        } else {
            designacao.setConfirmadoPeloVoluntario(false);
        }
        if (request.observacoes() != null) {
            designacao.setObservacoes(request.observacoes());
        }

        return toVoluntarioResponse(escalaVoluntarioRepository.save(designacao));
    }

    @Transactional
    public EscalaResponse.EscalaVoluntarioResponse responder(
            UUID escalaId,
            UUID designacaoId,
            EscalaVoluntarioRespostaRequest request
    ) {
        Escala escalaRef = buscarEntidade(escalaId);
        if (escalaRef.getStatus() != StatusEscala.EM_ANDAMENTO) {
            throw new BusinessException("Só é possível responder à designação após a escala ser lançada");
        }
        EscalaVoluntario designacao = escalaVoluntarioRepository.findById(designacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Designação não encontrada"));

        if (!designacao.getEscalaId().equals(escalaId)) {
            throw new BusinessException("A designação não pertence a esta escala");
        }

        Usuario usuario = autorizacaoService.requireUsuario();
        if (usuario.getPessoaId() == null || !usuario.getPessoaId().equals(designacao.getPessoaId())) {
            throw new AccessDeniedException("Só é possível responder à própria designação");
        }

        StatusEscalaVoluntario status = request.status();
        if (status != StatusEscalaVoluntario.CONFIRMADO && status != StatusEscalaVoluntario.RECUSADO) {
            throw new BusinessException("O voluntário só pode confirmar ou recusar a designação");
        }
        if (status == StatusEscalaVoluntario.RECUSADO
                && (request.observacoes() == null || request.observacoes().isBlank())) {
            throw new BusinessException("Informe o motivo da recusa");
        }

        designacao.setStatus(status);
        designacao.setConfirmadoPeloVoluntario(status == StatusEscalaVoluntario.CONFIRMADO);
        if (request.observacoes() != null) {
            designacao.setObservacoes(request.observacoes());
        }

        return toVoluntarioResponse(escalaVoluntarioRepository.save(designacao));
    }

    @Transactional
    public EscalaResponse.EscalaVoluntarioResponse atualizarDesignacao(
            UUID escalaId,
            UUID designacaoId,
            EscalaVoluntarioUpdateRequest request
    ) {
        Escala escalaRef = buscarEntidade(escalaId);
        assertPodeMutar(escalaRef);
        EscalaVoluntario designacao = escalaVoluntarioRepository.findById(designacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Designação não encontrada"));

        if (!designacao.getEscalaId().equals(escalaId)) {
            throw new BusinessException("A designação não pertence a esta escala");
        }

        Pessoa pessoa = pessoaService.buscarEntidade(request.pessoaId());
        autorizacaoService.assertPessoaDaIgreja(pessoa, escalaRef.getIgrejaId());
        Servico servico = servicoService.buscarEntidade(request.servicoId());

        escalaServicoRepository.findByEscalaIdAndServicoId(escalaId, servico.getId())
                .orElseThrow(() -> new BusinessException("Este serviço não está previsto nesta escala"));

        if (pessoaServicoRepository.findByPessoaIdAndServicoId(pessoa.getId(), servico.getId())
                .filter(v -> Boolean.TRUE.equals(v.getAtivo()))
                .isEmpty()) {
            throw new BusinessException("A pessoa não está habilitada para este serviço");
        }

        if (!designacao.getPessoaId().equals(pessoa.getId()) || !designacao.getServicoId().equals(servico.getId())) {
            boolean jaDesignada = escalaVoluntarioRepository
                    .findByEscalaIdAndPessoaIdAndServicoId(escalaId, pessoa.getId(), servico.getId())
                    .stream()
                    .anyMatch(outro -> !outro.getId().equals(designacaoId));
            if (jaDesignada) {
                throw new BusinessException("A pessoa já está designada para este serviço nesta escala");
            }
        }

        designacao.setPessoaId(pessoa.getId());
        designacao.setServicoId(servico.getId());
        designacao.setStatus(request.status());
        designacao.setObservacoes(request.observacoes());

        return toVoluntarioResponse(escalaVoluntarioRepository.save(designacao));
    }

    @Transactional
    public void removerDesignacao(UUID escalaId, UUID designacaoId) {
        Escala escalaRef = buscarEntidade(escalaId);
        assertPodeMutar(escalaRef);
        EscalaVoluntario designacao = escalaVoluntarioRepository.findById(designacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Designação não encontrada"));

        if (!designacao.getEscalaId().equals(escalaId)) {
            throw new BusinessException("A designação não pertence a esta escala");
        }

        escalaVoluntarioRepository.delete(designacao);
    }

    @Transactional
    public void excluir(UUID id) {
        Escala escala = buscarEntidade(id);
        assertPodeMutar(escala);
        escalaRepository.delete(escala);
    }

    private void assertPodeMutar(Escala escala) {
        autorizacaoService.assertPodeMutarEscala(
                escala.getTipo(), escala.getIgrejaId(), escala.getCompetencia(), escala.getMinisterioId()
        );
    }

    @Transactional(readOnly = true)
    public Escala buscarEntidade(UUID id) {
        return escalaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escala não encontrada"));
    }

    private void persistirItem(Escala escala, EscalaCreateRequest.EscalaItemRequest item) {
        if (item.pessoaId() == null) {
            return;
        }
        garantirPessoaDaIgreja(item.pessoaId(), escala.getIgrejaId());
        if (item.servicoId() != null) {
            garantirServicoDaIgreja(item.servicoId(), escala.getIgrejaId());
            escalaServicoRepository.findByEscalaIdAndServicoId(escala.getId(), item.servicoId())
                    .orElseGet(() -> {
                        EscalaServico escalaServico = new EscalaServico();
                        escalaServico.setEscalaId(escala.getId());
                        escalaServico.setServicoId(item.servicoId());
                        escalaServico.setQuantidadeNecessaria(1);
                        return escalaServicoRepository.save(escalaServico);
                    });
        }
        if (item.eventoId() != null) {
            cronogramaService.buscarEntidade(item.eventoId());
        }

        EscalaVoluntario designacao = new EscalaVoluntario();
        designacao.setEscalaId(escala.getId());
        designacao.setPessoaId(item.pessoaId());
        designacao.setServicoId(item.servicoId());
        designacao.setDataItem(item.data());
        designacao.setEventoId(item.eventoId());
        designacao.setHorario(item.horario());
        designacao.setStatus(StatusEscalaVoluntario.PENDENTE);
        escalaVoluntarioRepository.save(designacao);
    }

    private void garantirPessoaDaIgreja(UUID pessoaId, UUID igrejaId) {
        if (pessoaId == null) {
            return;
        }
        Pessoa pessoa = pessoaService.buscarEntidade(pessoaId);
        autorizacaoService.assertPessoaDaIgreja(pessoa, igrejaId);
    }

    private void garantirMinisterioDaIgreja(UUID ministerioId, UUID igrejaId) {
        Ministerio ministerio = ministerioRepository.findById(ministerioId)
                .orElseThrow(() -> new ResourceNotFoundException("Ministério não encontrado"));
        if (!ministerio.getIgrejaId().equals(igrejaId)) {
            throw new BusinessException("O ministério não pertence a esta igreja");
        }
    }

    private void garantirServicoDaIgreja(UUID servicoId, UUID igrejaId) {
        Servico servico = servicoService.buscarEntidade(servicoId);
        garantirMinisterioDaIgreja(servico.getMinisterioId(), igrejaId);
    }

    private String validarCompetencia(String competencia) {
        try {
            YearMonth.parse(competencia);
            return competencia;
        } catch (DateTimeParseException ex) {
            throw new BusinessException("Competência inválida. Use o formato YYYY-MM");
        }
    }

    private void validarJanelaAbertura(String competencia) {
        if (!janelaAberturaAberta(competencia, LocalDate.now())) {
            YearMonth mes = YearMonth.parse(competencia);
            LocalDate abertura = mes.minusMonths(1).atDay(20);
            throw new BusinessException(
                    "A escala da competência " + competencia + " abre no dia " + abertura
                            + " (20 do mês anterior). Aguarde a abertura da janela."
            );
        }
    }

    boolean janelaAberturaAberta(String competencia, LocalDate hoje) {
        YearMonth mes = YearMonth.parse(competencia);
        LocalDate abertura = mes.minusMonths(1).atDay(20);
        return !hoje.isBefore(abertura);
    }

    private void validarEvento(UUID eventoId) {
        if (eventoId != null) {
            cronogramaService.buscarEntidade(eventoId);
        }
    }

    private EscalaResponse toResponse(Escala escala, UUID pessoaFiltro) {
        return toResponses(List.of(escala), pessoaFiltro).get(0);
    }

    /** Monta as respostas carregando serviços, designações, ministérios e nomes em lote. */
    private List<EscalaResponse> toResponses(List<Escala> escalas, UUID pessoaFiltro) {
        if (escalas.isEmpty()) {
            return List.of();
        }
        List<UUID> escalaIds = escalas.stream().map(Escala::getId).toList();
        Map<UUID, List<EscalaServico>> servicosPorEscala = escalaServicoRepository.findByEscalaIdIn(escalaIds)
                .stream()
                .collect(Collectors.groupingBy(EscalaServico::getEscalaId));
        Map<UUID, List<EscalaVoluntario>> designacoesPorEscala = escalaVoluntarioRepository.findByEscalaIdIn(escalaIds)
                .stream()
                .filter(d -> pessoaFiltro == null || pessoaFiltro.equals(d.getPessoaId()))
                .collect(Collectors.groupingBy(EscalaVoluntario::getEscalaId));

        Set<UUID> pessoaIds = new HashSet<>();
        Set<UUID> servicoIds = new HashSet<>();
        escalas.forEach(e -> {
            pessoaIds.add(e.getSupervisorPessoaId());
            pessoaIds.add(e.getPregadorPessoaId());
        });
        servicosPorEscala.values().forEach(lista -> lista.forEach(es -> servicoIds.add(es.getServicoId())));
        designacoesPorEscala.values().forEach(lista -> lista.forEach(d -> {
            pessoaIds.add(d.getPessoaId());
            servicoIds.add(d.getServicoId());
        }));
        Nomes nomes = nomes(pessoaIds, servicoIds);

        List<UUID> ministerioIds = escalas.stream()
                .map(Escala::getMinisterioId).filter(Objects::nonNull).distinct().toList();
        Map<UUID, String> ministerios = ministerioIds.isEmpty()
                ? Map.of()
                : ministerioRepository.findAllById(ministerioIds).stream()
                .collect(Collectors.toMap(Ministerio::getId, Ministerio::getNome));

        return escalas.stream().map(escala -> {
            List<EscalaVoluntario> designacoes = designacoesPorEscala.getOrDefault(escala.getId(), List.of());
            return new EscalaResponse(
                    escala.getId(),
                    escala.getTitulo(),
                    escala.getDescricao(),
                    escala.getCompetencia(),
                    escala.getTipo(),
                    escala.getIgrejaId(),
                    escala.getMinisterioId(),
                    escala.getMinisterioId() == null ? null : ministerios.get(escala.getMinisterioId()),
                    escala.getSupervisorPessoaId(),
                    nomes.pessoa(escala.getSupervisorPessoaId()),
                    escala.getPregadorPessoaId(),
                    nomes.pessoa(escala.getPregadorPessoaId()),
                    escala.getDataEvento(),
                    escala.getHoraInicio(),
                    escala.getHoraFim(),
                    escala.getLocalEvento(),
                    escala.getEventoId(),
                    escala.getAtivo(),
                    escala.getStatus(),
                    escala.getDataLancamento(),
                    escala.getDataFinalizacao(),
                    designacoes.stream().map(d -> toItemResponse(d, nomes)).toList(),
                    servicosPorEscala.getOrDefault(escala.getId(), List.of()).stream()
                            .map(es -> new EscalaResponse.EscalaServicoResponse(
                                    es.getId(),
                                    es.getServicoId(),
                                    nomes.servico(es.getServicoId()),
                                    es.getQuantidadeNecessaria()))
                            .toList(),
                    designacoes.stream()
                            .map(d -> toVoluntarioResponse(d, nomes.pessoa(d.getPessoaId()), nomes.servico(d.getServicoId())))
                            .toList(),
                    escala.getCreatedAt(),
                    escala.getUpdatedAt()
            );
        }).toList();
    }

    private EscalaResponse.EscalaItemResponse toItemResponse(EscalaVoluntario designacao, Nomes nomes) {
        return new EscalaResponse.EscalaItemResponse(
                designacao.getId(),
                designacao.getDataItem(),
                designacao.getEventoId(),
                designacao.getPessoaId(),
                nomes.pessoa(designacao.getPessoaId()),
                designacao.getServicoId(),
                nomes.servico(designacao.getServicoId()),
                designacao.getHorario(),
                designacao.getStatus(),
                designacao.getConfirmadoPeloVoluntario()
        );
    }

    private EscalaResponse.EscalaVoluntarioResponse toVoluntarioResponse(EscalaVoluntario designacao) {
        return toVoluntarioResponse(designacao, nomePessoa(designacao.getPessoaId()), servicoNome(designacao.getServicoId()));
    }

    private EscalaResponse.EscalaVoluntarioResponse toVoluntarioResponse(
            EscalaVoluntario designacao,
            String pessoaNome,
            String servicoNome
    ) {
        return new EscalaResponse.EscalaVoluntarioResponse(
                designacao.getId(),
                designacao.getPessoaId(),
                pessoaNome,
                designacao.getServicoId(),
                servicoNome,
                designacao.getStatus(),
                designacao.getConfirmadoPeloVoluntario(),
                designacao.getObservacoes(),
                designacao.getCreatedAt(),
                designacao.getUpdatedAt()
        );
    }

    private String nomePessoa(UUID pessoaId) {
        if (pessoaId == null) {
            return null;
        }
        return pessoaService.buscarEntidade(pessoaId).getNome();
    }

    private Nomes nomes(Collection<UUID> pessoaIds, Collection<UUID> servicoIds) {
        return new Nomes(pessoaService.nomesPorId(pessoaIds), servicoService.nomesPorId(servicoIds));
    }

    /** Nomes de pessoas e serviços já carregados em lote para montar respostas. */
    private record Nomes(Map<UUID, String> pessoas, Map<UUID, String> servicos) {
        String pessoa(UUID id) {
            return id == null ? null : pessoas.get(id);
        }

        String servico(UUID id) {
            return id == null ? null : servicos.get(id);
        }
    }
}
