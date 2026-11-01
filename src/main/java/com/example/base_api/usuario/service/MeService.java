package com.example.base_api.usuario.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.igreja.dto.IgrejaResponse;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.entity.IgrejaTipo;
import com.example.base_api.igreja.service.IgrejaContextoService;
import com.example.base_api.igreja.service.IgrejaService;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.usuario.dto.MeResponse;
import com.example.base_api.usuario.dto.MeUpdateRequest;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class MeService {

    private final UsuarioRepository usuarioRepository;
    private final PessoaRepository pessoaRepository;
    private final PasswordEncoder passwordEncoder;
    private final IgrejaContextoService igrejaContextoService;
    private final IgrejaService igrejaService;

    public MeService(
            UsuarioRepository usuarioRepository,
            PessoaRepository pessoaRepository,
            PasswordEncoder passwordEncoder,
            IgrejaContextoService igrejaContextoService,
            IgrejaService igrejaService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.pessoaRepository = pessoaRepository;
        this.passwordEncoder = passwordEncoder;
        this.igrejaContextoService = igrejaContextoService;
        this.igrejaService = igrejaService;
    }

    @Transactional(readOnly = true)
    public MeResponse obterPerfil(Authentication authentication) {
        Usuario usuario = buscarUsuarioAutenticado(authentication);
        Pessoa pessoa = resolverPessoa(usuario);
        return toResponse(usuario, pessoa);
    }

    @Transactional
    public MeResponse atualizarPerfil(Authentication authentication, MeUpdateRequest request) {
        Usuario usuario = buscarUsuarioAutenticado(authentication);

        usuarioRepository.findByEmailIgnoreCase(request.email()).ifPresent(outro -> {
            if (!outro.getId().equals(usuario.getId())) {
                throw new BusinessException("E-mail já está em uso");
            }
        });

        usuario.setEmail(request.email().trim().toLowerCase());
        if (request.novaSenha() != null && !request.novaSenha().isBlank()) {
            if (request.senhaAtual() == null || request.senhaAtual().isBlank()
                    || !passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
                throw new BusinessException("Senha atual incorreta");
            }
            usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
            usuario.invalidarTokens();
        }
        usuarioRepository.save(usuario);

        Pessoa pessoa = resolverPessoa(usuario);
        if (pessoa != null) {
            pessoa.setNome(request.nome().trim());
            pessoa.setNomeSocial(blankToNull(request.nomeSocial()));
            pessoa.setEmail(request.email().trim().toLowerCase());
            pessoa.setTelefone(blankToNull(request.telefone()));
            pessoa.setDataNascimento(request.dataNascimento());
            pessoa.setObservacoes(request.observacoes());
            pessoaRepository.save(pessoa);
        }

        return toResponse(usuario, pessoa);
    }

    private Usuario buscarUsuarioAutenticado(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException("Usuário não autenticado");
        }
        return usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado"));
    }

    private Pessoa resolverPessoa(Usuario usuario) {
        if (usuario.getPessoaId() == null) {
            return null;
        }
        return pessoaRepository.findById(usuario.getPessoaId()).orElse(null);
    }

    private MeResponse toResponse(Usuario usuario, Pessoa pessoa) {
        Set<String> papeis = usuario.nomesPapeisDaIgrejaAtual();

        Igreja igreja = igrejaContextoService.igrejaAtual();
        List<IgrejaResponse> visiveis = igrejaContextoService.igrejasVisiveis().stream()
                .map(igrejaService::toResponse)
                .toList();

        IgrejaTipo tipo = igreja == null ? null : igreja.getTipo();
        String igrejaNome = igreja == null ? null : igreja.getNome();

        return new MeResponse(
                usuario.getId(),
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getAtivo(),
                papeis,
                pessoa != null ? pessoa.getId() : usuario.getPessoaId(),
                pessoa != null ? pessoa.getNome() : null,
                pessoa != null ? pessoa.getNomeSocial() : null,
                pessoa != null ? pessoa.getTelefone() : null,
                pessoa != null ? pessoa.getDataNascimento() : null,
                pessoa != null ? pessoa.getObservacoes() : null,
                usuario.getIgrejaId(),
                tipo,
                igrejaNome,
                visiveis
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
