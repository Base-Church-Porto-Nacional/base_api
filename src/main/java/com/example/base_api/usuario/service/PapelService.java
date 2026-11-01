package com.example.base_api.usuario.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.usuario.dto.PapelRequest;
import com.example.base_api.usuario.dto.PapelResponse;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.repository.PapelRepository;
import org.springframework.security.access.AccessDeniedException;
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
    public List<PapelResponse> listar(UUID igrejaIdSolicitada) {
        UUID igrejaId = igrejaIdSolicitada != null ? igrejaIdSolicitada : autorizacaoService.igrejaAtualId();
        autorizacaoService.assertAcesso(igrejaId);
        return papelRepository.findByIgrejaIdIsNullOrIgrejaId(igrejaId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PapelResponse buscarPorId(UUID id) {
        Papel papel = buscarEntidade(id);
        assertVisivel(papel);
        return toResponse(papel);
    }

    @Transactional
    public PapelResponse criar(PapelRequest request) {
        UUID igrejaId = request.igrejaId() != null ? request.igrejaId() : autorizacaoService.igrejaAtualId();
        autorizacaoService.assertPastorOuAdmin(igrejaId);
        String nome = normalizarNome(request.nome());
        if (papelRepository.existsByNomeAndIgrejaId(nome, igrejaId)) {
            throw new ConflictException("Já existe um papel com este nome nesta igreja");
        }

        Papel papel = new Papel();
        papel.setNome(nome);
        papel.setDescricao(request.descricao());
        papel.setPodeLancarContagem(Boolean.TRUE.equals(request.podeLancarContagem()));
        papel.setIgrejaId(igrejaId);
        return toResponse(papelRepository.save(papel));
    }

    @Transactional
    public PapelResponse atualizar(UUID id, PapelRequest request) {
        Papel papel = buscarEntidade(id);
        assertCustomizadoDaIgreja(papel);
        UUID igrejaId = papel.getIgrejaId();
        String nome = normalizarNome(request.nome());

        papelRepository.findByNomeAndIgrejaId(nome, igrejaId).ifPresent(outro -> {
            if (!outro.getId().equals(id)) {
                throw new BusinessException("Já existe um papel com este nome nesta igreja");
            }
        });

        papel.setNome(nome);
        papel.setDescricao(request.descricao());
        papel.setPodeLancarContagem(Boolean.TRUE.equals(request.podeLancarContagem()));
        return toResponse(papelRepository.save(papel));
    }

    @Transactional
    public void excluir(UUID id) {
        Papel papel = buscarEntidade(id);
        assertCustomizadoDaIgreja(papel);
        papelRepository.delete(papel);
    }

    @Transactional(readOnly = true)
    public Papel buscarEntidade(UUID id) {
        return papelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Papel não encontrado"));
    }

    private String normalizarNome(String nomeBruto) {
        String nome = nomeBruto.trim().toUpperCase();
        rejeitarPapeisProibidos(nome);
        if (PAPEIS_SISTEMA.contains(nome)) {
            throw new BusinessException("Papel de sistema não pode ser criado ou renomeado. Use a promoção de gestão ou os atalhos.");
        }
        return nome;
    }

    private void rejeitarPapeisProibidos(String nome) {
        if (PAPEIS_PROIBIDOS.contains(nome)) {
            throw new BusinessException("Papel " + nome + " não é permitido. Use atribuição de culto/escala ou serviço ficção.");
        }
    }

    /** Um papel é visível se for de sistema (global) ou pertencer a uma igreja que o usuário enxerga. */
    private void assertVisivel(Papel papel) {
        if (papel.isSistema()) {
            return;
        }
        autorizacaoService.assertAcesso(papel.getIgrejaId());
    }

    /** Só pastor/admin da igreja dona pode mutar um papel customizado; papéis de sistema são imutáveis. */
    private void assertCustomizadoDaIgreja(Papel papel) {
        if (papel.isSistema()) {
            throw new AccessDeniedException("Papel de sistema não pode ser alterado ou excluído");
        }
        autorizacaoService.assertPastorOuAdmin(papel.getIgrejaId());
    }

    private PapelResponse toResponse(Papel papel) {
        return new PapelResponse(
                papel.getId(),
                papel.getNome(),
                papel.getDescricao(),
                papel.getIgrejaId(),
                papel.isSistema(),
                papel.isPodeLancarContagem(),
                papel.getCreatedAt(),
                papel.getUpdatedAt()
        );
    }
}
