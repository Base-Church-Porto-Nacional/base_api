package com.example.base_api.usuario.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.security.JwtService;
import com.example.base_api.usuario.dto.LoginRequest;
import com.example.base_api.usuario.dto.LoginResponse;
import com.example.base_api.usuario.dto.UsuarioCreateRequest;
import com.example.base_api.usuario.dto.UsuarioResponse;
import com.example.base_api.usuario.dto.UsuarioUpdateRequest;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.PapelRepository;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PapelRepository papelRepository;
    private final PessoaService pessoaService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PapelRepository papelRepository,
            PessoaService pessoaService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.usuarioRepository = usuarioRepository;
        this.papelRepository = papelRepository;
        this.pessoaService = pessoaService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(UUID id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioCreateRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username já está em uso");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("E-mail já está em uso");
        }
        if (request.pessoaId() != null) {
            pessoaService.buscarEntidade(request.pessoaId());
            if (usuarioRepository.existsByPessoaId(request.pessoaId())) {
                throw new BusinessException("Esta pessoa já possui usuário vinculado");
            }
        }

        Usuario usuario = new Usuario();
        usuario.setPessoaId(request.pessoaId());
        usuario.setUsername(request.username().trim());
        usuario.setEmail(request.email().trim().toLowerCase());
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setAtivo(true);
        usuario.setPapeis(resolverPapeis(request.papeis()));

        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse atualizar(UUID id, UsuarioUpdateRequest request) {
        Usuario usuario = buscarEntidade(id);

        usuarioRepository.findByEmailIgnoreCase(request.email()).ifPresent(outro -> {
            if (!outro.getId().equals(id)) {
                throw new BusinessException("E-mail já está em uso");
            }
        });

        usuario.setEmail(request.email().trim().toLowerCase());
        usuario.setAtivo(request.ativo());
        usuario.setPapeis(resolverPapeis(request.papeis()));

        if (request.novaSenha() != null && !request.novaSenha().isBlank()) {
            usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        }

        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public void excluir(UUID id) {
        Usuario usuario = buscarEntidade(id);
        usuarioRepository.delete(usuario);
    }

    @Transactional(readOnly = true)
    public LoginResponse autenticar(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.senha())
        );

        Usuario usuario = usuarioRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new BusinessException("Usuário inativo");
        }

        Set<String> papeis = usuario.getPapeis().stream()
                .map(Papel::getNome)
                .collect(Collectors.toSet());

        String token = jwtService.gerarToken(usuario.getUsername(), papeis);

        return new LoginResponse(token, "Bearer", usuario.getId(), usuario.getUsername(), papeis);
    }

    @Transactional(readOnly = true)
    public Usuario buscarEntidade(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private Set<Papel> resolverPapeis(Set<String> nomes) {
        Set<Papel> papeis = new HashSet<>();
        for (String nome : nomes) {
            Papel papel = papelRepository.findByNome(nome.toUpperCase())
                    .orElseThrow(() -> new BusinessException("Papel inválido: " + nome));
            papeis.add(papel);
        }
        return papeis;
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        Set<String> papeis = usuario.getPapeis().stream()
                .map(Papel::getNome)
                .collect(Collectors.toSet());

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getPessoaId(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getAtivo(),
                papeis,
                usuario.getCreatedAt(),
                usuario.getUpdatedAt()
        );
    }
}
