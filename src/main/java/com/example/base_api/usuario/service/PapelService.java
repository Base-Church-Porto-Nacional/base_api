package com.example.base_api.usuario.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.usuario.dto.PapelRequest;
import com.example.base_api.usuario.dto.PapelResponse;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.repository.PapelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PapelService {

    private final PapelRepository papelRepository;

    public PapelService(PapelRepository papelRepository) {
        this.papelRepository = papelRepository;
    }

    @Transactional(readOnly = true)
    public List<PapelResponse> listar() {
        return papelRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PapelResponse buscarPorId(UUID id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public PapelResponse criar(PapelRequest request) {
        String nome = request.nome().trim().toUpperCase();
        papelRepository.findByNome(nome).ifPresent(p -> {
            throw new BusinessException("Já existe um papel com este nome");
        });

        Papel papel = new Papel();
        papel.setNome(nome);
        papel.setDescricao(request.descricao());
        return toResponse(papelRepository.save(papel));
    }

    @Transactional
    public PapelResponse atualizar(UUID id, PapelRequest request) {
        Papel papel = buscarEntidade(id);
        String nome = request.nome().trim().toUpperCase();

        papelRepository.findByNome(nome).ifPresent(outro -> {
            if (!outro.getId().equals(id)) {
                throw new BusinessException("Já existe um papel com este nome");
            }
        });

        papel.setNome(nome);
        papel.setDescricao(request.descricao());
        return toResponse(papelRepository.save(papel));
    }

    @Transactional
    public void excluir(UUID id) {
        Papel papel = buscarEntidade(id);
        papelRepository.delete(papel);
    }

    @Transactional(readOnly = true)
    public Papel buscarEntidade(UUID id) {
        return papelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Papel não encontrado"));
    }

    private PapelResponse toResponse(Papel papel) {
        return new PapelResponse(
                papel.getId(),
                papel.getNome(),
                papel.getDescricao(),
                papel.getCreatedAt(),
                papel.getUpdatedAt()
        );
    }
}
