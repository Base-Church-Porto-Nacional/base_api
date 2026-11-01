package com.example.base_api.igreja.service;

import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.common.exception.UnauthorizedException;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.security.UsuarioAutenticado;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
        // Por id, a consulta é servida pelo cache da sessão JPA dentro da mesma transação.
        Optional<Usuario> usuario = authentication.getPrincipal() instanceof UsuarioAutenticado principal
                ? usuarioRepository.findById(principal.getUsuarioId())
                : usuarioRepository.findByUsername(authentication.getName());
        return usuario.orElseThrow(() -> new UnauthorizedException("Usuário autenticado não encontrado"));
    }

    public Igreja igrejaAtual() {
        return igrejaAtual(requireUsuario());
    }

    public UUID igrejaAtualId() {
        return igrejaAtual().getId();
    }

    public List<Igreja> igrejasVisiveis() {
        return igrejasVisiveis(igrejaAtual());
    }

    public List<UUID> igrejasVisiveisIds() {
        return ids(igrejasVisiveis());
    }

    public void assertAcesso(UUID igrejaId) {
        assertAcesso(requireUsuario(), igrejaId);
    }

    public List<UUID> idsParaListagem(UUID igrejaIdSolicitado) {
        if (igrejaIdSolicitado != null) {
            assertAcesso(igrejaIdSolicitado);
            return List.of(igrejaIdSolicitado);
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
        return isPastorOuAdmin(requireUsuario(), igrejaId);
    }

    public boolean isMatriz() {
        return igrejaAtual().isMatriz();
    }

    public void assertPastorOuAdmin(UUID igrejaId) {
        Usuario usuario = requireUsuario();
        assertAcesso(usuario, igrejaId);
        if (!isPastorOuAdmin(usuario, igrejaId)) {
            throw new AccessDeniedException("Somente pastor ou admin desta igreja");
        }
    }

    public void assertMatrizPastorOuAdmin() {
        Usuario usuario = requireUsuario();
        Igreja atual = igrejaAtual(usuario);
        if (!atual.isMatriz() || !isPastorOuAdmin(usuario, atual.getId())) {
            throw new AccessDeniedException("Somente pastor ou admin da matriz");
        }
    }

    private Igreja igrejaAtual(Usuario usuario) {
        if (usuario.getIgrejaId() == null) {
            throw new AccessDeniedException("Igreja obrigatória no contexto");
        }
        return buscarIgreja(usuario.getIgrejaId());
    }

    private List<Igreja> igrejasVisiveis(Igreja atual) {
        if (!atual.isMatriz()) {
            return List.of(atual);
        }
        List<Igreja> rede = new ArrayList<>();
        rede.add(atual);
        rede.addAll(igrejaRepository.findByIgrejaMatrizIdOrderByNomeAsc(atual.getId()));
        return rede;
    }

    private void assertAcesso(Usuario usuario, UUID igrejaId) {
        if (igrejaId == null) {
            throw new AccessDeniedException("Igreja obrigatória no contexto");
        }
        if (!ids(igrejasVisiveis(igrejaAtual(usuario))).contains(igrejaId)) {
            throw new AccessDeniedException("Sem acesso a esta igreja");
        }
    }

    private boolean isPastorOuAdmin(Usuario usuario, UUID igrejaId) {
        Set<String> papeis = papeisNaIgreja(usuario, igrejaId);
        return papeis.contains("ADMIN") || papeis.contains("PASTOR");
    }

    private static List<UUID> ids(List<Igreja> igrejas) {
        return igrejas.stream().map(Igreja::getId).toList();
    }

    public Igreja buscarIgreja(UUID id) {
        return igrejaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Igreja não encontrada"));
    }
}
