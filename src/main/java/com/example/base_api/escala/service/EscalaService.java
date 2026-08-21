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
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaServicoRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.service.ServicoService;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public EscalaService(
            EscalaRepository escalaRepository,
            EscalaServicoRepository escalaServicoRepository,
            EscalaVoluntarioRepository escalaVoluntarioRepository,
            ServicoService servicoService,
            PessoaService pessoaService,
            PessoaServicoRepository pessoaServicoRepository,
            CronogramaService cronogramaService
    ) {
        this.escalaRepository = escalaRepository;
        this.escalaServicoRepository = escalaServicoRepository;
        this.escalaVoluntarioRepository = escalaVoluntarioRepository;
        this.servicoService = servicoService;
        this.pessoaService = pessoaService;
        this.pessoaServicoRepository = pessoaServicoRepository;
        this.cronogramaService = cronogramaService;
    }

    @Transactional(readOnly = true)
    public List<EscalaResponse> listar() {
        return escalaRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EscalaResponse buscarPorId(UUID id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public EscalaResponse criar(EscalaCreateRequest request) {
        validarHorario(request.horaInicio(), request.horaFim());
        validarEvento(request.eventoId());

        Escala escala = new Escala();
        escala.setTitulo(request.titulo().trim());
        escala.setDescricao(request.descricao());
        escala.setDataEvento(request.dataEvento());
        escala.setHoraInicio(request.horaInicio());
        escala.setHoraFim(request.horaFim());
        escala.setLocalEvento(request.localEvento());
        escala.setEventoId(request.eventoId());
        escala.setAtivo(true);

        Escala salva = escalaRepository.save(escala);

        for (EscalaCreateRequest.EscalaServicoRequest item : request.servicos()) {
            servicoService.buscarEntidade(item.servicoId());
            EscalaServico escalaServico = new EscalaServico();
            escalaServico.setEscalaId(salva.getId());
            escalaServico.setServicoId(item.servicoId());
            escalaServico.setQuantidadeNecessaria(item.quantidadeNecessaria());
            escalaServicoRepository.save(escalaServico);
        }

        return toResponse(salva);
    }

    @Transactional
    public EscalaResponse atualizar(UUID id, EscalaUpdateRequest request) {
        Escala escala = buscarEntidade(id);
        validarHorario(request.horaInicio(), request.horaFim());
        validarEvento(request.eventoId());

        escala.setTitulo(request.titulo().trim());
        escala.setDescricao(request.descricao());
        escala.setDataEvento(request.dataEvento());
        escala.setHoraInicio(request.horaInicio());
        escala.setHoraFim(request.horaFim());
        escala.setLocalEvento(request.localEvento());
        escala.setEventoId(request.eventoId());
        escala.setAtivo(request.ativo());

        return toResponse(escalaRepository.save(escala));
    }

    @Transactional
    public EscalaResponse.EscalaServicoResponse adicionarServico(UUID escalaId, EscalaServicoUpsertRequest request) {
        Escala escala = buscarEntidade(escalaId);
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
        buscarEntidade(escalaId);
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
        buscarEntidade(escalaId);
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
        Pessoa pessoa = pessoaService.buscarEntidade(request.pessoaId());
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
        buscarEntidade(escalaId);
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
        buscarEntidade(escalaId);
        EscalaVoluntario designacao = escalaVoluntarioRepository.findById(designacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Designação não encontrada"));

        if (!designacao.getEscalaId().equals(escalaId)) {
            throw new BusinessException("A designação não pertence a esta escala");
        }

        Pessoa pessoa = pessoaService.buscarEntidade(request.pessoaId());
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
        buscarEntidade(escalaId);
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
        escalaRepository.delete(escala);
    }

    @Transactional(readOnly = true)
    public Escala buscarEntidade(UUID id) {
        return escalaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escala não encontrada"));
    }

    private void validarHorario(java.time.LocalTime inicio, java.time.LocalTime fim) {
        if (fim != null && !fim.isAfter(inicio)) {
            throw new BusinessException("Hora fim deve ser posterior à hora início");
        }
    }

    private void validarEvento(UUID eventoId) {
        if (eventoId != null) {
            cronogramaService.buscarEntidade(eventoId);
        }
    }

    private EscalaResponse toResponse(Escala escala) {
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

        List<EscalaResponse.EscalaVoluntarioResponse> voluntarios = escalaVoluntarioRepository
                .findByEscalaId(escala.getId())
                .stream()
                .map(this::toVoluntarioResponse)
                .toList();

        return new EscalaResponse(
                escala.getId(),
                escala.getTitulo(),
                escala.getDescricao(),
                escala.getDataEvento(),
                escala.getHoraInicio(),
                escala.getHoraFim(),
                escala.getLocalEvento(),
                escala.getEventoId(),
                escala.getAtivo(),
                servicos,
                voluntarios,
                escala.getCreatedAt(),
                escala.getUpdatedAt()
        );
    }

    private EscalaResponse.EscalaVoluntarioResponse toVoluntarioResponse(EscalaVoluntario designacao) {
        Pessoa pessoa = pessoaService.buscarEntidade(designacao.getPessoaId());
        Servico servico = servicoService.buscarEntidade(designacao.getServicoId());
        return new EscalaResponse.EscalaVoluntarioResponse(
                designacao.getId(),
                designacao.getPessoaId(),
                pessoa.getNome(),
                designacao.getServicoId(),
                servico.getNome(),
                designacao.getStatus(),
                designacao.getObservacoes(),
                designacao.getCreatedAt(),
                designacao.getUpdatedAt()
        );
    }
}
