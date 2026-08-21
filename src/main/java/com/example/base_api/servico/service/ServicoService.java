package com.example.base_api.servico.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.ministerio.service.MinisterioService;
import com.example.base_api.servico.dto.ServicoCreateRequest;
import com.example.base_api.servico.dto.ServicoResponse;
import com.example.base_api.servico.dto.ServicoUpdateRequest;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.repository.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final MinisterioService ministerioService;

    public ServicoService(ServicoRepository servicoRepository, MinisterioService ministerioService) {
        this.servicoRepository = servicoRepository;
        this.ministerioService = ministerioService;
    }

    @Transactional(readOnly = true)
    public List<ServicoResponse> listarPorMinisterio(UUID ministerioId) {
        ministerioService.buscarEntidade(ministerioId);
        return servicoRepository.findByMinisterioIdOrderByNomeAsc(ministerioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServicoResponse buscarPorId(UUID id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public ServicoResponse criar(UUID ministerioId, ServicoCreateRequest request) {
        ministerioService.buscarEntidade(ministerioId);

        servicoRepository.findByMinisterioIdAndNomeIgnoreCase(ministerioId, request.nome()).ifPresent(s -> {
            throw new BusinessException("Já existe um serviço com este nome neste ministério");
        });

        Servico servico = new Servico();
        servico.setMinisterioId(ministerioId);
        servico.setNome(request.nome().trim());
        servico.setDescricao(request.descricao());
        servico.setAtivo(true);

        return toResponse(servicoRepository.save(servico));
    }

    @Transactional
    public ServicoResponse atualizar(UUID id, ServicoUpdateRequest request) {
        Servico servico = buscarEntidade(id);

        if (servicoRepository.existsByMinisterioIdAndNomeIgnoreCaseAndIdNot(
                servico.getMinisterioId(), request.nome(), id)) {
            throw new BusinessException("Já existe um serviço com este nome neste ministério");
        }

        servico.setNome(request.nome().trim());
        servico.setDescricao(request.descricao());
        servico.setAtivo(request.ativo());

        return toResponse(servicoRepository.save(servico));
    }

    @Transactional
    public void excluir(UUID id) {
        Servico servico = buscarEntidade(id);
        servicoRepository.delete(servico);
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
                servico.getAtivo(),
                servico.getCreatedAt(),
                servico.getUpdatedAt()
        );
    }
}
