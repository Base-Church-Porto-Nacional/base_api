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
import java.util.Set;
import java.util.UUID;

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
        return usuarioRepository.findAtivosByPapelAndIgrejaIdIn("LIDER", ids).stream()
                .map(this::toResponse)
                .toList();
    }

    private LiderResponse toResponse(Usuario usuario) {
        Pessoa pessoa = usuario.getPessoaId() == null
                ? null
                : pessoaRepository.findById(usuario.getPessoaId()).orElse(null);

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
