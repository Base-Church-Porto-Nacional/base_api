package com.example.base_api.igreja.service;

import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.common.exception.UnauthorizedException;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class IgrejaContextoService {

    private final UsuarioRepository usuarioRepository;
    private final IgrejaRepository igrejaRepository;

    public IgrejaContextoService(UsuarioRepository usuarioRepository, IgrejaRepository igrejaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.igrejaRepository = igrejaRepository;
    }

    public Usuario requireUsuario() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication.getName() == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new UnauthorizedException("Usuário não autenticado");
        }
        return usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Usuário autenticado não encontrado"));
    }

    public Igreja igrejaAtual() {
        Usuario usuario = requireUsuario();
        if (usuario.getIgrejaId() == null) {
            throw new AccessDeniedException("Igreja obrigatória no contexto");
        }
        return buscarIgreja(usuario.getIgrejaId());
    }

    public UUID igrejaAtualId() {
        return igrejaAtual().getId();
    }

    public List<Igreja> igrejasVisiveis() {
        Igreja igreja = igrejaAtual();
        if (igreja.isMatriz()) {
            List<Igreja> rede = new ArrayList<>();
            rede.add(igreja);
            rede.addAll(igrejaRepository.findByIgrejaMatrizIdOrderByNomeAsc(igreja.getId()));
            return rede;
        }
        return List.of(igreja);
    }

    public List<UUID> igrejasVisiveisIds() {
        return igrejasVisiveis().stream().map(Igreja::getId).toList();
    }

    public void assertAcesso(UUID igrejaId) {
        requireUsuario();
        if (igrejaId == null) {
            throw new AccessDeniedException("Igreja obrigatória no contexto");
        }
        if (!igrejasVisiveisIds().contains(igrejaId)) {
            throw new AccessDeniedException("Sem acesso a esta igreja");
        }
    }

    public List<UUID> idsParaListagem(UUID igrejaIdSolicitado) {
        if (igrejaIdSolicitado != null) {
            assertAcesso(igrejaIdSolicitado);
            return List.of(igrejaIdSolicitado);
        }
        Igreja atual = igrejaAtual();
        if (!atual.isMatriz()) {
            return List.of(atual.getId());
        }
        return igrejasVisiveisIds();
    }

    public Set<String> papeisNaIgreja(UUID igrejaId) {
        return papeisNaIgreja(requireUsuario(), igrejaId);
    }

    public Set<String> papeisNaIgreja(Usuario usuario, UUID igrejaId) {
        if (igrejaId == null) {
            return Set.of();
        }
        Set<String> papeis = new HashSet<>(usuario.nomesPapeis(igrejaId));
        Igreja alvo = igrejaRepository.findById(igrejaId).orElse(null);
        if (alvo != null && !alvo.isMatriz() && alvo.getIgrejaMatrizId() != null) {
            usuario.nomesPapeis(alvo.getIgrejaMatrizId()).stream()
                    .filter(nome -> "ADMIN".equals(nome) || "PASTOR".equals(nome))
                    .forEach(papeis::add);
        }
        return papeis;
    }

    public boolean isPastorOuAdmin(UUID igrejaId) {
        Set<String> papeis = papeisNaIgreja(igrejaId);
        return papeis.contains("ADMIN") || papeis.contains("PASTOR");
    }

    public boolean isPastorOuAdmin() {
        return isPastorOuAdmin(igrejaAtualId());
    }

    public boolean isMatriz() {
        return igrejaAtual().isMatriz();
    }

    public boolean temPapel(String papel) {
        return temPapel(igrejaAtualId(), papel);
    }

    public boolean temPapel(UUID igrejaId, String papel) {
        return papeisNaIgreja(igrejaId).contains(papel);
    }

    public void assertPastorOuAdmin(UUID igrejaId) {
        assertAcesso(igrejaId);
        if (!isPastorOuAdmin(igrejaId)) {
            throw new AccessDeniedException("Somente pastor ou admin desta igreja");
        }
    }

    public void assertMatrizPastorOuAdmin() {
        Igreja atual = igrejaAtual();
        if (!atual.isMatriz() || !isPastorOuAdmin(atual.getId())) {
            throw new AccessDeniedException("Somente pastor ou admin da matriz");
        }
    }

    public Igreja buscarIgreja(UUID id) {
        return igrejaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Igreja não encontrada"));
    }
}
