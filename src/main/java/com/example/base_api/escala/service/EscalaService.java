package com.example.base_api.escala.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.cronograma.service.CronogramaService;
import com.example.base_api.escala.dto.EscalaCreateRequest;
import com.example.base_api.escala.dto.EscalaResponse;
import com.example.base_api.escala.dto.EscalaServicoUpsertRequest;
import com.example.base_api.escala.dto.EscalaUpdateRequest;
import com.example.base_api.escala.dto.EscalaVoluntarioCreateRequest;
import com.example.base_api.escala.dto.EscalaVoluntarioStatusRequest;
import com.example.base_api.escala.dto.EscalaVoluntarioUpdateRequest;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.EscalaServico;
import com.example.base_api.escala.entity.EscalaVoluntario;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Service
public class EscalaService {

    private final EscalaRepository escalaRepository;
    private final EscalaServicoRepository escalaServicoRepository;
    private final EscalaVoluntarioRepository escalaVoluntarioRepository;
    private final ServicoService servicoService;
    private final PessoaService pessoaService;
    private final PessoaServicoRepository pessoaServicoRepository;
    private final CronogramaService cronogramaService;
    private final IgrejaContextoService igrejaContextoService;
    private final AutorizacaoService autorizacaoService;
    private final MinisterioRepository ministerioRepository;

    public EscalaService(
            EscalaRepository escalaRepository,
            EscalaServicoRepository escalaServicoRepository,
            EscalaVoluntarioRepository escalaVoluntarioRepository,
            ServicoService servicoService,
            PessoaService pessoaService,
            PessoaServicoRepository pessoaServicoRepository,
            CronogramaService cronogramaService,
            IgrejaContextoService igrejaContextoService,
            AutorizacaoService autorizacaoService,
            MinisterioRepository ministerioRepository
    ) {
        this.escalaRepository = escalaRepository;
        this.escalaServicoRepository = escalaServicoRepository;
        this.escalaVoluntarioRepository = escalaVoluntarioRepository;
        this.servicoService = servicoService;
        this.pessoaService = pessoaService;
        this.pessoaServicoRepository = pessoaServicoRepository;
        this.cronogramaService = cronogramaService;
        this.igrejaContextoService = igrejaContextoService;
        this.autorizacaoService = autorizacaoService;
        this.ministerioRepository = ministerioRepository;
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
        UUID pessoaNaResposta = pessoaFiltro;
        return escalas.stream().map(e -> toResponse(e, pessoaNaResposta)).toList();
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

    @Transactional
    public EscalaResponse criar(EscalaCreateRequest request) {
        autorizacaoService.assertAcesso(request.igrejaId());
        String competencia = validarCompetencia(request.competencia());
        TipoEscala tipo = request.tipo();
        autorizacaoService.assertPodeCriarEscala(tipo, request.igrejaId(), competencia, request.ministerioId());

        if (tipo == TipoEscala.MINISTERIO && request.ministerioId() == null) {
            throw new BusinessException("ministerioId é obrigatório para escala de ministério");
        }
        if (request.ministerioId() != null) {
            Ministerio ministerio = ministerioRepository.findById(request.ministerioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ministério não encontrado"));
            if (!ministerio.getIgrejaId().equals(request.igrejaId())) {
                throw new BusinessException("O ministério não pertence a esta igreja");
            }
        }

        validarHorario(request.horaInicio(), request.horaFim());
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

        Escala salva = escalaRepository.save(escala);

        if (request.servicos() != null) {
            for (EscalaCreateRequest.EscalaServicoRequest item : request.servicos()) {
                servicoService.buscarEntidade(item.servicoId());
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

        return toResponse(salva, null);
    }

    @Transactional
    public EscalaResponse atualizar(UUID id, EscalaUpdateRequest request) {
        Escala escala = buscarEntidade(id);
        autorizacaoService.assertPodeMutarEscala(
                escala.getTipo(), escala.getIgrejaId(), escala.getCompetencia(), escala.getMinisterioId()
        );
        validarHorario(request.horaInicio(), request.horaFim());
        validarEvento(request.eventoId());

        escala.setTitulo(request.titulo().trim());
        escala.setDescricao(request.descricao());
        if (request.competencia() != null && !request.competencia().isBlank()) {
            escala.setCompetencia(validarCompetencia(request.competencia()));
        }
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
    public EscalaResponse.EscalaServicoResponse adicionarServico(UUID escalaId, EscalaServicoUpsertRequest request) {
        Escala escala = buscarEntidade(escalaId);
        assertPodeMutar(escala);
        servicoService.buscarEntidade(request.servicoId());

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

        servicoService.buscarEntidade(request.servicoId());

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

        escalaVoluntarioRepository
                .findByEscalaIdAndPessoaIdAndServicoId(escala.getId(), pessoa.getId(), servico.getId())
                .ifPresent(v -> {
                    throw new BusinessException("A pessoa já está designada para este serviço nesta escala");
                });

        long ocupados = escalaVoluntarioRepository.countByEscalaIdAndServicoIdAndStatusIn(
                escala.getId(),
                servico.getId(),
                List.of(StatusEscalaVoluntario.PENDENTE, StatusEscalaVoluntario.CONFIRMADO)
        );
        if (ocupados >= necessidade.getQuantidadeNecessaria()) {
            throw new BusinessException("Quantidade necessária para este serviço já foi preenchida");
        }

        EscalaVoluntario designacao = new EscalaVoluntario();
        designacao.setEscalaId(escala.getId());
        designacao.setPessoaId(pessoa.getId());
        designacao.setServicoId(servico.getId());
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
            escalaVoluntarioRepository
                    .findByEscalaIdAndPessoaIdAndServicoId(escalaId, pessoa.getId(), servico.getId())
                    .filter(outro -> !outro.getId().equals(designacaoId))
                    .ifPresent(outro -> {
                        throw new BusinessException("A pessoa já está designada para este serviço nesta escala");
                    });
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
            servicoService.buscarEntidade(item.servicoId());
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

    private String validarCompetencia(String competencia) {
        try {
            YearMonth.parse(competencia);
            return competencia;
        } catch (DateTimeParseException ex) {
            throw new BusinessException("Competência inválida. Use o formato YYYY-MM");
        }
    }

    private void validarHorario(LocalTime inicio, LocalTime fim) {
        if (inicio != null && fim != null && !fim.isAfter(inicio)) {
            throw new BusinessException("Hora fim deve ser posterior à hora início");
        }
    }

    private void validarEvento(UUID eventoId) {
        if (eventoId != null) {
            cronogramaService.buscarEntidade(eventoId);
        }
    }

    private EscalaResponse toResponse(Escala escala, UUID pessoaFiltro) {
        List<EscalaResponse.EscalaServicoResponse> servicos = escalaServicoRepository
                .findByEscalaId(escala.getId())
                .stream()
                .map(item -> {
                    Servico servico = servicoService.buscarEntidade(item.getServicoId());
                    return new EscalaResponse.EscalaServicoResponse(
                            item.getId(),
                            item.getServicoId(),
                            servico.getNome(),
                            item.getQuantidadeNecessaria()
                    );
                })
                .toList();

        List<EscalaVoluntario> designacoes = escalaVoluntarioRepository.findByEscalaId(escala.getId());
        if (pessoaFiltro != null) {
            designacoes = designacoes.stream()
                    .filter(d -> pessoaFiltro.equals(d.getPessoaId()))
                    .toList();
        }

        List<EscalaResponse.EscalaVoluntarioResponse> voluntarios = designacoes.stream()
                .map(this::toVoluntarioResponse)
                .toList();

        List<EscalaResponse.EscalaItemResponse> itens = designacoes.stream()
                .map(this::toItemResponse)
                .toList();

        String ministerioNome = escala.getMinisterioId() == null
                ? null
                : ministerioRepository.findById(escala.getMinisterioId())
                .map(Ministerio::getNome)
                .orElse(null);

        return new EscalaResponse(
                escala.getId(),
                escala.getTitulo(),
                escala.getDescricao(),
                escala.getCompetencia(),
                escala.getTipo(),
                escala.getIgrejaId(),
                escala.getMinisterioId(),
                ministerioNome,
                escala.getSupervisorPessoaId(),
                nomePessoa(escala.getSupervisorPessoaId()),
                escala.getPregadorPessoaId(),
                nomePessoa(escala.getPregadorPessoaId()),
                escala.getDataEvento(),
                escala.getHoraInicio(),
                escala.getHoraFim(),
                escala.getLocalEvento(),
                escala.getEventoId(),
                escala.getAtivo(),
                itens,
                servicos,
                voluntarios,
                escala.getCreatedAt(),
                escala.getUpdatedAt()
        );
    }

    private EscalaResponse.EscalaItemResponse toItemResponse(EscalaVoluntario designacao) {
        Pessoa pessoa = pessoaService.buscarEntidade(designacao.getPessoaId());
        String servicoNome = null;
        if (designacao.getServicoId() != null) {
            servicoNome = servicoService.buscarEntidade(designacao.getServicoId()).getNome();
        }
        return new EscalaResponse.EscalaItemResponse(
                designacao.getId(),
                designacao.getDataItem(),
                designacao.getEventoId(),
                designacao.getPessoaId(),
                pessoa.getNome(),
                designacao.getServicoId(),
                servicoNome,
                designacao.getHorario(),
                designacao.getStatus()
        );
    }

    private EscalaResponse.EscalaVoluntarioResponse toVoluntarioResponse(EscalaVoluntario designacao) {
        Pessoa pessoa = pessoaService.buscarEntidade(designacao.getPessoaId());
        String servicoNome = null;
        if (designacao.getServicoId() != null) {
            servicoNome = servicoService.buscarEntidade(designacao.getServicoId()).getNome();
        }
        return new EscalaResponse.EscalaVoluntarioResponse(
                designacao.getId(),
                designacao.getPessoaId(),
                pessoa.getNome(),
                designacao.getServicoId(),
                servicoNome,
                designacao.getStatus(),
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
}
