package com.example.base_api.usuario.service;

import com.example.base_api.baseministerial.repository.BaseMinisterialRepository;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.smallgroup.repository.SmallGroupRepository;
import com.example.base_api.usuario.dto.LiderResponse;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LiderService {

    private final UsuarioRepository usuarioRepository;
    private final PessoaRepository pessoaRepository;
    private final MinisterioRepository ministerioRepository;
    private final BaseMinisterialRepository baseMinisterialRepository;
    private final SmallGroupRepository smallGroupRepository;
    private final AutorizacaoService autorizacaoService;

    public LiderService(
            UsuarioRepository usuarioRepository,
            PessoaRepository pessoaRepository,
            MinisterioRepository ministerioRepository,
            BaseMinisterialRepository baseMinisterialRepository,
            SmallGroupRepository smallGroupRepository,
            AutorizacaoService autorizacaoService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.pessoaRepository = pessoaRepository;
        this.ministerioRepository = ministerioRepository;
        this.baseMinisterialRepository = baseMinisterialRepository;
        this.smallGroupRepository = smallGroupRepository;
        this.autorizacaoService = autorizacaoService;
    }

    /**
     * Líder é quem lidera/coordena um ministério ou uma base ministerial, ou lidera um pequeno
     * grupo, nas igrejas informadas. Considera apenas pessoas ativas; cada pessoa conta uma vez.
     */
    @Transactional(readOnly = true)
    public Set<UUID> pessoaIdsLideres(List<UUID> igrejaIds) {
        return lideres(igrejaIds).stream().map(Pessoa::getId).collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public List<LiderResponse> listar(UUID igrejaId) {
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId);
        List<Pessoa> pessoas = lideres(ids);
        if (pessoas.isEmpty()) {
            return List.of();
        }
        Map<UUID, Usuario> usuarios = usuarioRepository
                .findByPessoaIdIn(pessoas.stream().map(Pessoa::getId).toList()).stream()
                .collect(Collectors.toMap(Usuario::getPessoaId, Function.identity(), (a, b) -> a));
        return pessoas.stream()
                .sorted(Comparator.comparing(Pessoa::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(p -> toResponse(usuarios.get(p.getId()), p))
                .toList();
    }

    private List<Pessoa> lideres(List<UUID> igrejaIds) {
        if (igrejaIds == null || igrejaIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> candidatos = new HashSet<>();
        candidatos.addAll(ministerioRepository.idsLideres(igrejaIds));
        candidatos.addAll(ministerioRepository.idsCoordenadores(igrejaIds));
        candidatos.addAll(baseMinisterialRepository.idsLideres(igrejaIds));
        candidatos.addAll(baseMinisterialRepository.idsCoordenadores(igrejaIds));
        candidatos.addAll(smallGroupRepository.idsLideres(igrejaIds));
        candidatos.removeIf(Objects::isNull);
        if (candidatos.isEmpty()) {
            return List.of();
        }
        return pessoaRepository.findByIdInAndAtivoTrue(candidatos);
    }

    private LiderResponse toResponse(Usuario usuario, Pessoa pessoa) {
        return new LiderResponse(
                usuario != null ? usuario.getId() : null,
                usuario != null ? usuario.getUsername() : null,
                usuario != null ? usuario.getEmail() : null,
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getTelefone(),
                usuario != null ? usuario.nomesPapeisDaIgrejaAtual() : Set.of()
        );
    }
}
