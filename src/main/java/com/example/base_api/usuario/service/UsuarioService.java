package com.example.base_api.usuario.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.security.Capacidade;
import com.example.base_api.security.JwtService;
import com.example.base_api.security.Papeis;
import com.example.base_api.usuario.dto.LoginRequest;
import com.example.base_api.usuario.dto.LoginResponse;
import com.example.base_api.usuario.dto.PessoaUsuarioCreateRequest;
import com.example.base_api.usuario.dto.UsuarioCreateRequest;
import com.example.base_api.usuario.dto.UsuarioPapeisRequest;
import com.example.base_api.usuario.dto.UsuarioResponse;
import com.example.base_api.usuario.dto.UsuarioUpdateRequest;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.entity.UsuarioPapel;
import com.example.base_api.usuario.repository.PapelRepository;
import com.example.base_api.usuario.repository.UsuarioPapelRepository;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PapelRepository papelRepository;
    private final UsuarioPapelRepository usuarioPapelRepository;
    private final PessoaService pessoaService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AutorizacaoService autorizacaoService;
    private final IgrejaRepository igrejaRepository;
    private final AuditService auditService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PapelRepository papelRepository,
            UsuarioPapelRepository usuarioPapelRepository,
            PessoaService pessoaService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            AutorizacaoService autorizacaoService,
            IgrejaRepository igrejaRepository,
            AuditService auditService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.papelRepository = papelRepository;
        this.usuarioPapelRepository = usuarioPapelRepository;
        this.pessoaService = pessoaService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.autorizacaoService = autorizacaoService;
        this.igrejaRepository = igrejaRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(UUID igrejaId) {
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId).stream()
                .filter(autorizacaoService::isGestao)
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Usuario> usuarios = usuarioRepository.findByIgrejaIdIn(ids);
        Map<UUID, String> pessoas = pessoaService.nomesPorId(usuarios.stream().map(Usuario::getPessoaId).toList());
        List<UUID> igrejaIds = usuarios.stream().map(Usuario::getIgrejaId).filter(Objects::nonNull).distinct().toList();
        Map<UUID, String> igrejas = igrejaIds.isEmpty()
                ? Map.of()
                : igrejaRepository.findAllById(igrejaIds).stream()
                .collect(Collectors.toMap(Igreja::getId, Igreja::getNome));
        return usuarios.stream()
                .map(u -> toResponse(
                        u,
                        u.getPessoaId() == null ? null : pessoas.get(u.getPessoaId()),
                        u.getIgrejaId() == null ? null : igrejas.get(u.getIgrejaId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(UUID id) {
        Usuario usuario = buscarEntidade(id);
        autorizacaoService.assertGestao(usuario.getIgrejaId());
        return toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse criar(UsuarioCreateRequest request) {
        UUID igrejaId = request.igrejaId() != null
                ? request.igrejaId()
                : autorizacaoService.requireUsuario().getIgrejaId();
        if (igrejaId == null) {
            throw new AccessDeniedException("Igreja obrigatória no contexto");
        }
        autorizacaoService.assertPodeCriarUsuario(igrejaId);
        return persistir(
                request.pessoaId(),
                request.username(),
                request.email(),
                request.senha(),
                igrejaId
        );
    }

    @Transactional
    public UsuarioResponse criarAPartirDaPessoa(UUID pessoaId, PessoaUsuarioCreateRequest request) {
        Pessoa pessoa = pessoaService.buscarEntidade(pessoaId);
        UUID igrejaId = request.igrejaId() != null ? request.igrejaId() : pessoa.getIgrejaId();
        if (igrejaId == null) {
            throw new AccessDeniedException("Igreja obrigatória no contexto");
        }
        autorizacaoService.assertPodeCriarUsuario(igrejaId);
        autorizacaoService.assertAcesso(pessoa.getIgrejaId());
        return persistir(pessoaId, request.username(), request.email(), request.senha(), igrejaId);
    }

    @Transactional
    public UsuarioResponse atualizar(UUID id, UsuarioUpdateRequest request) {
        Usuario usuario = buscarEntidade(id);
        autorizacaoService.assertGestao(usuario.getIgrejaId());

        usuarioRepository.findByEmailIgnoreCase(request.email()).ifPresent(outro -> {
            if (!outro.getId().equals(id)) {
                throw new BusinessException("E-mail já está em uso");
            }
        });

        usuario.setEmail(request.email().trim().toLowerCase());
        boolean invalidaToken = false;
        if (request.ativo() != null && !request.ativo().equals(usuario.getAtivo())) {
            invalidaToken = true;
        }
        usuario.setAtivo(request.ativo());

        if (request.novaSenha() != null && !request.novaSenha().isBlank()) {
            usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
            invalidaToken = true;
        }
        if (invalidaToken) {
            usuario.invalidarTokens();
        }

        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse alterarPapeis(UUID id, UsuarioPapeisRequest request) {
        Usuario usuario = buscarEntidade(id);
        UUID igrejaId = usuario.getIgrejaId();
        if (igrejaId == null) {
            throw new AccessDeniedException("Igreja obrigatória no contexto");
        }
        Set<String> novos = new HashSet<>();
        for (String nome : request.papeis()) {
            String normalizado = nome.trim().toUpperCase();
            if (!Papeis.GESTAO.contains(normalizado)
                    && papelRepository.findByNomeAndIgrejaId(normalizado, igrejaId).isEmpty()) {
                throw new BusinessException("Papel inválido: " + nome);
            }
            novos.add(normalizado);
        }
        Set<String> atuais = new HashSet<>(usuario.nomesPapeis(igrejaId));

        boolean mudaGestao = !gestaoDe(atuais).equals(gestaoDe(novos));
        if (mudaGestao) {
            autorizacaoService.assertPodeAtribuirPapeisGestao(igrejaId);
        } else {
            autorizacaoService.assertTem(igrejaId, Capacidade.PAPEIS_CUSTOM_GERIR);
        }
        if (atuais.contains(Papeis.PASTOR) && !novos.contains(Papeis.PASTOR)
                && usuarioRepository.countByPapelAndIgrejaId(Papeis.PASTOR, igrejaId) <= 1) {
            throw new BusinessException("A igreja precisa de ao menos um pastor");
        }

        substituirPapeis(usuario, igrejaId, novos);
        usuario.invalidarTokens();
        Usuario salvo = usuarioRepository.save(usuario);
        auditService.registrar(
                "ALTERAR_PAPEIS",
                "usuario",
                "usuarioId=" + salvo.getId() + " igrejaId=" + igrejaId
                        + " antes=" + new TreeSet<>(atuais) + " depois=" + new TreeSet<>(novos)
        );
        return toResponse(salvo);
    }

    private static Set<String> gestaoDe(Set<String> nomes) {
        return nomes.stream().filter(Papeis.GESTAO::contains).collect(Collectors.toSet());
    }

    @Transactional
    public void excluir(UUID id) {
        Usuario usuario = buscarEntidade(id);
        autorizacaoService.assertGestao(usuario.getIgrejaId());
        usuarioRepository.excluirDireto(id);
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

        Igreja igreja = usuario.getIgrejaId() == null
                ? null
                : igrejaRepository.findById(usuario.getIgrejaId()).orElse(null);
        String igrejaTipo = igreja == null ? null : igreja.getTipo().name();

        String token = jwtService.gerarToken(
                usuario.getUsername(),
                usuario.getPessoaId(),
                usuario.getIgrejaId(),
                igrejaTipo,
                usuario.getTokenVersion() == null ? 0 : usuario.getTokenVersion()
        );

        return new LoginResponse(
                token,
                "Bearer",
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPessoaId(),
                usuario.getIgrejaId(),
                igrejaTipo
        );
    }

    @Transactional(readOnly = true)
    public Usuario buscarEntidade(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private UsuarioResponse persistir(
            UUID pessoaId,
            String username,
            String email,
            String senha,
            UUID igrejaId
    ) {
        if (usuarioRepository.existsByUsername(username)) {
            throw new BusinessException("Username já está em uso");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("E-mail já está em uso");
        }
        if (pessoaId != null) {
            pessoaService.buscarEntidade(pessoaId);
            autorizacaoService.assertPessoaSemUsuario(usuarioRepository.existsByPessoaId(pessoaId));
        }

        Usuario usuario = new Usuario();
        usuario.setPessoaId(pessoaId);
        usuario.setIgrejaId(igrejaId);
        usuario.setUsername(username.trim());
        usuario.setEmail(email.trim().toLowerCase());
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setAtivo(true);

        return toResponse(usuarioRepository.save(usuario));
    }

    public void substituirPapeis(Usuario usuario, UUID igrejaId, Set<String> nomes) {
        atribuirPapeis(usuario, igrejaId, resolverPapeis(nomes, igrejaId));
    }

    private void atribuirPapeis(Usuario usuario, UUID igrejaId, Set<Papel> papeis) {
        usuarioPapelRepository.deleteByUsuarioIdAndIgrejaId(usuario.getId(), igrejaId);
        usuarioPapelRepository.flush();
        if (usuario.getPapeisIgreja() != null) {
            usuario.getPapeisIgreja().removeIf(up -> igrejaId.equals(up.getIgrejaId()));
        }
        for (Papel papel : papeis) {
            UsuarioPapel vinculo = new UsuarioPapel();
            vinculo.setUsuarioId(usuario.getId());
            vinculo.setPapelId(papel.getId());
            vinculo.setIgrejaId(igrejaId);
            vinculo.setPapel(papel);
            usuarioPapelRepository.save(vinculo);
            usuario.getPapeisIgreja().add(vinculo);
        }
    }

    private Set<Papel> resolverPapeis(Set<String> nomes, UUID igrejaId) {
        Set<Papel> papeis = new HashSet<>();
        for (String nome : nomes) {
            String normalizado = nome.toUpperCase();
            Papel papel = papelRepository.findByNomeAndIgrejaIdIsNull(normalizado)
                    .or(() -> papelRepository.findByNomeAndIgrejaId(normalizado, igrejaId))
                    .orElseThrow(() -> new BusinessException("Papel inválido: " + nome));
            papeis.add(papel);
        }
        return papeis;
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        String pessoaNome = usuario.getPessoaId() == null
                ? null
                : pessoaService.buscarEntidade(usuario.getPessoaId()).getNome();
        String igrejaNome = usuario.getIgrejaId() == null
                ? null
                : igrejaRepository.findById(usuario.getIgrejaId()).map(Igreja::getNome).orElse(null);
        return toResponse(usuario, pessoaNome, igrejaNome);
    }

    private UsuarioResponse toResponse(Usuario usuario, String pessoaNome, String igrejaNome) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getPessoaId(),
                pessoaNome,
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getIgrejaId(),
                igrejaNome,
                usuario.getAtivo(),
                usuario.nomesPapeisDaIgrejaAtual(),
                usuario.getCreatedAt(),
                usuario.getUpdatedAt()
        );
    }
}
