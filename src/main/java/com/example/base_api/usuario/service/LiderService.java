package com.example.base_api.usuario.service;

import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.usuario.dto.LiderResponse;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final AutorizacaoService autorizacaoService;

    public LiderService(
            UsuarioRepository usuarioRepository,
            PessoaRepository pessoaRepository,
            AutorizacaoService autorizacaoService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.pessoaRepository = pessoaRepository;
        this.autorizacaoService = autorizacaoService;
    }

    @Transactional(readOnly = true)
    public List<LiderResponse> listar(UUID igrejaId) {
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId);
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Usuario> lideres = usuarioRepository.findAtivosByPapelAndIgrejaIdIn("LIDER", ids);
        List<UUID> pessoaIds = lideres.stream().map(Usuario::getPessoaId).filter(Objects::nonNull).distinct().toList();
        Map<UUID, Pessoa> pessoas = pessoaIds.isEmpty()
                ? Map.of()
                : pessoaRepository.findAllById(pessoaIds).stream()
                .collect(Collectors.toMap(Pessoa::getId, Function.identity()));
        return lideres.stream()
                .map(u -> toResponse(u, u.getPessoaId() == null ? null : pessoas.get(u.getPessoaId())))
                .toList();
    }

    private LiderResponse toResponse(Usuario usuario, Pessoa pessoa) {
        Set<String> papeis = usuario.nomesPapeisDaIgrejaAtual();

        return new LiderResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                pessoa != null ? pessoa.getId() : usuario.getPessoaId(),
                pessoa != null ? pessoa.getNome() : null,
                pessoa != null ? pessoa.getTelefone() : null,
                papeis
        );
    }
}
