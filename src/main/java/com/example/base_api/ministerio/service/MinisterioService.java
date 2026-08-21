package com.example.base_api.ministerio.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.ministerio.dto.MinisterioCreateRequest;
import com.example.base_api.ministerio.dto.MinisterioResponse;
import com.example.base_api.ministerio.dto.MinisterioUpdateRequest;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MinisterioService {

    private final MinisterioRepository ministerioRepository;

    public MinisterioService(MinisterioRepository ministerioRepository) {
        this.ministerioRepository = ministerioRepository;
    }

    @Transactional(readOnly = true)
    public List<MinisterioResponse> listar() {
        return ministerioRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MinisterioResponse buscarPorId(UUID id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public MinisterioResponse criar(MinisterioCreateRequest request) {
        ministerioRepository.findByNomeIgnoreCase(request.nome()).ifPresent(m -> {
            throw new BusinessException("Já existe um ministério com este nome");
        });

        Ministerio ministerio = new Ministerio();
        ministerio.setNome(request.nome().trim());
        ministerio.setDescricao(request.descricao());
        ministerio.setAtivo(true);

        return toResponse(ministerioRepository.save(ministerio));
    }

    @Transactional
    public MinisterioResponse atualizar(UUID id, MinisterioUpdateRequest request) {
        Ministerio ministerio = buscarEntidade(id);

        if (ministerioRepository.existsByNomeIgnoreCaseAndIdNot(request.nome(), id)) {
            throw new BusinessException("Já existe um ministério com este nome");
        }

        ministerio.setNome(request.nome().trim());
        ministerio.setDescricao(request.descricao());
        ministerio.setAtivo(request.ativo());

        return toResponse(ministerioRepository.save(ministerio));
    }

    @Transactional
    public void excluir(UUID id) {
        Ministerio ministerio = buscarEntidade(id);
        ministerioRepository.delete(ministerio);
    }

    @Transactional(readOnly = true)
    public Ministerio buscarEntidade(UUID id) {
        return ministerioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ministério não encontrado"));
    }

    public MinisterioResponse toResponse(Ministerio ministerio) {
        return new MinisterioResponse(
                ministerio.getId(),
                ministerio.getNome(),
                ministerio.getDescricao(),
                ministerio.getAtivo(),
                ministerio.getCreatedAt(),
                ministerio.getUpdatedAt()
        );
    }
}
