package com.example.base_api.usuario.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.usuario.dto.PapelRequest;
import com.example.base_api.usuario.dto.PapelResponse;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.repository.PapelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PapelService {

    private static final Set<String> PAPEIS_SISTEMA = Set.of("ADMIN", "PASTOR", "LIDER", "VOLUNTARIO", "MEMBRO");
    private static final Set<String> PAPEIS_PROIBIDOS = Set.of("COORDENADOR", "SUPERVISOR", "PREGADOR");

    private final PapelRepository papelRepository;
    private final AutorizacaoService autorizacaoService;

    public PapelService(PapelRepository papelRepository, AutorizacaoService autorizacaoService) {
        this.papelRepository = papelRepository;
        this.autorizacaoService = autorizacaoService;
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
        autorizacaoService.assertPastorOuAdmin(autorizacaoService.requireUsuario().getIgrejaId());
        String nome = request.nome().trim().toUpperCase();
        rejeitarPapeisProibidos(nome);
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
        autorizacaoService.assertPastorOuAdmin(autorizacaoService.requireUsuario().getIgrejaId());
        Papel papel = buscarEntidade(id);
        String nome = request.nome().trim().toUpperCase();
        rejeitarPapeisProibidos(nome);

        if (isPapelSistema(papel.getNome()) && !papel.getNome().equalsIgnoreCase(nome)) {
            throw new BusinessException("Papel de sistema não pode ser renomeado");
        }

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
        autorizacaoService.assertPastorOuAdmin(autorizacaoService.requireUsuario().getIgrejaId());
        Papel papel = buscarEntidade(id);
        if (isPapelSistema(papel.getNome())) {
            throw new BusinessException("Papel de sistema não pode ser excluído");
        }
        papelRepository.delete(papel);
    }

    @Transactional(readOnly = true)
    public Papel buscarEntidade(UUID id) {
        return papelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Papel não encontrado"));
    }

    private void rejeitarPapeisProibidos(String nome) {
        if (PAPEIS_PROIBIDOS.contains(nome)) {
            throw new BusinessException("Papel " + nome + " não é permitido. Use atribuição de culto/escala ou serviço ficção.");
        }
    }

    private boolean isPapelSistema(String nome) {
        return nome != null && PAPEIS_SISTEMA.contains(nome.toUpperCase());
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
