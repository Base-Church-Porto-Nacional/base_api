package com.example.base_api.servico.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.ministerio.service.MinisterioService;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.servico.dto.ServicoCreateRequest;
import com.example.base_api.servico.dto.ServicoResponse;
import com.example.base_api.servico.dto.ServicoUpdateRequest;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.repository.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final MinisterioService ministerioService;
    private final AutorizacaoService autorizacaoService;

    public ServicoService(
            ServicoRepository servicoRepository,
            MinisterioService ministerioService,
            AutorizacaoService autorizacaoService
    ) {
        this.servicoRepository = servicoRepository;
        this.ministerioService = ministerioService;
        this.autorizacaoService = autorizacaoService;
    }

    @Transactional(readOnly = true)
    public List<ServicoResponse> listarPorMinisterio(UUID ministerioId) {
        autorizacaoService.assertAcesso(ministerioService.buscarEntidade(ministerioId).getIgrejaId());
        return servicoRepository.findByMinisterioIdOrderByNomeAsc(ministerioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServicoResponse buscarPorId(UUID id) {
        Servico servico = buscarEntidade(id);
        autorizacaoService.assertAcesso(ministerioService.buscarEntidade(servico.getMinisterioId()).getIgrejaId());
        return toResponse(servico);
    }

    @Transactional
    public ServicoResponse criar(UUID ministerioId, ServicoCreateRequest request) {
        autorizacaoService.assertPodeGerenciarMinisterio(ministerioService.buscarEntidade(ministerioId));

        servicoRepository.findByMinisterioIdAndNomeIgnoreCase(ministerioId, request.nome()).ifPresent(s -> {
            throw new BusinessException("Já existe um serviço com este nome neste ministério");
        });

        Servico servico = new Servico();
        servico.setMinisterioId(ministerioId);
        servico.setNome(request.nome().trim());
        servico.setDescricao(request.descricao());
        servico.setFiccao(Boolean.TRUE.equals(request.ficcao()));
        servico.setAtivo(true);

        return toResponse(servicoRepository.save(servico));
    }

    @Transactional
    public ServicoResponse atualizar(UUID id, ServicoUpdateRequest request) {
        Servico servico = buscarEntidade(id);
        autorizacaoService.assertPodeGerenciarMinisterio(ministerioService.buscarEntidade(servico.getMinisterioId()));

        if (servicoRepository.existsByMinisterioIdAndNomeIgnoreCaseAndIdNot(
                servico.getMinisterioId(), request.nome(), id)) {
            throw new BusinessException("Já existe um serviço com este nome neste ministério");
        }

        servico.setNome(request.nome().trim());
        servico.setDescricao(request.descricao());
        servico.setAtivo(request.ativo());
        if (request.ficcao() != null) {
            servico.setFiccao(request.ficcao());
        }

        return toResponse(servicoRepository.save(servico));
    }

    @Transactional
    public void excluir(UUID id) {
        Servico servico = buscarEntidade(id);
        autorizacaoService.assertPodeGerenciarMinisterio(ministerioService.buscarEntidade(servico.getMinisterioId()));
        servicoRepository.delete(servico);
    }

    /** Serviços por id, numa única consulta (ids nulos são ignorados). */
    @Transactional(readOnly = true)
    public Map<UUID, Servico> buscarPorIds(Collection<UUID> ids) {
        List<UUID> validos = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (validos.isEmpty()) {
            return Map.of();
        }
        return servicoRepository.findAllById(validos).stream()
                .collect(Collectors.toMap(Servico::getId, Function.identity()));
    }

    /** Nomes dos serviços por id, numa única consulta (ids nulos são ignorados). */
    @Transactional(readOnly = true)
    public Map<UUID, String> nomesPorId(Collection<UUID> ids) {
        return servicoRepository.nomesPorId(ids);
    }

    @Transactional(readOnly = true)
    public Servico buscarEntidade(UUID id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));
    }

    public ServicoResponse toResponse(Servico servico) {
        return new ServicoResponse(
                servico.getId(),
                servico.getMinisterioId(),
                servico.getNome(),
                servico.getDescricao(),
                servico.getFiccao(),
                servico.getAtivo(),
                servico.getCreatedAt(),
                servico.getUpdatedAt()
        );
    }
}
