package com.example.base_api.jornada.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.jornada.dto.JornadaEtapaCatalogoRequest;
import com.example.base_api.jornada.dto.JornadaEtapaCatalogoResponse;
import com.example.base_api.jornada.entity.JornadaEtapaCatalogo;
import com.example.base_api.jornada.repository.JornadaEtapaCatalogoRepository;
import com.example.base_api.security.AutorizacaoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class JornadaEtapaCatalogoService {

    private final JornadaEtapaCatalogoRepository repository;
    private final AutorizacaoService autorizacaoService;

    public JornadaEtapaCatalogoService(
            JornadaEtapaCatalogoRepository repository,
            AutorizacaoService autorizacaoService
    ) {
        this.repository = repository;
        this.autorizacaoService = autorizacaoService;
    }

    @Transactional(readOnly = true)
    public List<JornadaEtapaCatalogoResponse> listar() {
        autorizacaoService.requireUsuario();
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public JornadaEtapaCatalogoResponse buscarPorId(UUID id) {
        autorizacaoService.requireUsuario();
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public JornadaEtapaCatalogoResponse criar(JornadaEtapaCatalogoRequest request) {
        autorizacaoService.assertPastorOuAdmin(autorizacaoService.igrejaAtualId());
        String codigo = request.codigo().trim().toUpperCase();
        repository.findByCodigo(codigo).ifPresent(e -> {
            throw new BusinessException("Já existe uma etapa com este código");
        });

        JornadaEtapaCatalogo etapa = new JornadaEtapaCatalogo();
        etapa.setCodigo(codigo);
        etapa.setNome(request.nome().trim());
        etapa.setDescricao(request.descricao());
        etapa.setOrdem(request.ordem());
        etapa.setAtivo(request.ativo());

        return toResponse(repository.save(etapa));
    }

    @Transactional
    public JornadaEtapaCatalogoResponse atualizar(UUID id, JornadaEtapaCatalogoRequest request) {
        autorizacaoService.assertPastorOuAdmin(autorizacaoService.igrejaAtualId());
        JornadaEtapaCatalogo etapa = buscarEntidade(id);
        String codigo = request.codigo().trim().toUpperCase();

        repository.findByCodigo(codigo).ifPresent(outro -> {
            if (!outro.getId().equals(id)) {
                throw new BusinessException("Já existe uma etapa com este código");
            }
        });

        etapa.setCodigo(codigo);
        etapa.setNome(request.nome().trim());
        etapa.setDescricao(request.descricao());
        etapa.setOrdem(request.ordem());
        etapa.setAtivo(request.ativo());

        return toResponse(repository.save(etapa));
    }

    @Transactional
    public void excluir(UUID id) {
        autorizacaoService.assertPastorOuAdmin(autorizacaoService.igrejaAtualId());
        JornadaEtapaCatalogo etapa = buscarEntidade(id);
        repository.delete(etapa);
    }

    private JornadaEtapaCatalogo buscarEntidade(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Etapa do catálogo não encontrada"));
    }

    private JornadaEtapaCatalogoResponse toResponse(JornadaEtapaCatalogo etapa) {
        return new JornadaEtapaCatalogoResponse(
                etapa.getId(),
                etapa.getCodigo(),
                etapa.getNome(),
                etapa.getDescricao(),
                etapa.getOrdem(),
                etapa.getAtivo(),
                etapa.getCreatedAt(),
                etapa.getUpdatedAt()
        );
    }
}
