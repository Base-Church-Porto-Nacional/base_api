package com.example.base_api.usuario.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.security.JwtService;
import com.example.base_api.usuario.dto.LoginRequest;
import com.example.base_api.usuario.dto.LoginResponse;
import com.example.base_api.usuario.dto.PessoaUsuarioCreateRequest;
import com.example.base_api.usuario.dto.UsuarioCreateRequest;
import com.example.base_api.usuario.dto.UsuarioGestaoRequest;
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
import java.util.Set;
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
                .filter(autorizacaoService::isPastorOuAdmin)
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return usuarioRepository.findByIgrejaIdIn(ids).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(UUID id) {
        Usuario usuario = buscarEntidade(id);
        autorizacaoService.assertPastorOuAdmin(usuario.getIgrejaId());
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
        Set<String> papeis = validarPapeis(request.papeis(), igrejaId);
        return persistir(
                request.pessoaId(),
                request.username(),
                request.email(),
                request.senha(),
                papeis,
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
        Set<String> papeis = validarPapeis(request.papeis(), igrejaId);
        return persistir(pessoaId, request.username(), request.email(), request.senha(), papeis, igrejaId);
    }

    @Transactional
    public UsuarioResponse atualizar(UUID id, UsuarioUpdateRequest request) {
        Usuario usuario = buscarEntidade(id);
        autorizacaoService.assertPastorOuAdmin(usuario.getIgrejaId());

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

        Set<String> atalho = validarPapeis(request.papeis(), usuario.getIgrejaId());
        Set<String> preservados = usuario.nomesPapeis(usuario.getIgrejaId()).stream()
                .filter(AutorizacaoService.PAPEIS_GESTAO::contains)
                .collect(Collectors.toCollection(HashSet::new));
        preservados.addAll(atalho);
        substituirPapeis(usuario, usuario.getIgrejaId(), preservados);
        invalidaToken = true;

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
    public UsuarioResponse promoverGestao(UUID id, UsuarioGestaoRequest request) {
        Usuario usuario = buscarEntidade(id);
        UUID igrejaAlvo = request.igrejaId() != null ? request.igrejaId() : usuario.getIgrejaId();
        if (igrejaAlvo == null) {
            throw new AccessDeniedException("Igreja obrigatória no contexto");
        }
        autorizacaoService.assertPodePromoverGestao(igrejaAlvo);

        Set<String> atuais = new HashSet<>(usuario.nomesPapeis(igrejaAlvo));
        for (String nome : request.papeis()) {
            String papel = nome.toUpperCase();
            if (!AutorizacaoService.PAPEIS_GESTAO.contains(papel)) {
                throw new AccessDeniedException("Somente ADMIN ou PASTOR podem ser promovidos neste endpoint");
            }
            atuais.add(papel);
        }
        substituirPapeis(usuario, igrejaAlvo, atuais);
        usuario.invalidarTokens();
        Usuario salvo = usuarioRepository.save(usuario);
        auditService.registrar(
                "PROMOVER_GESTAO",
                "usuario",
                "usuarioId=" + salvo.getId() + " igrejaId=" + igrejaAlvo + " papeis=" + atuais
        );
        return toResponse(salvo);
    }

    @Transactional
    public void excluir(UUID id) {
        Usuario usuario = buscarEntidade(id);
        autorizacaoService.assertPastorOuAdmin(usuario.getIgrejaId());
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

        Set<String> papeis = usuario.nomesPapeisDaIgrejaAtual();

        Igreja igreja = usuario.getIgrejaId() == null
                ? null
                : igrejaRepository.findById(usuario.getIgrejaId()).orElse(null);
        String igrejaTipo = igreja == null ? null : igreja.getTipo().name();

        String token = jwtService.gerarToken(
                usuario.getUsername(),
                papeis,
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
                papeis,
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
            Set<String> papeis,
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

        Set<Papel> papeisResolvidos = resolverPapeis(papeis, igrejaId);

        Usuario usuario = new Usuario();
        usuario.setPessoaId(pessoaId);
        usuario.setIgrejaId(igrejaId);
        usuario.setUsername(username.trim());
        usuario.setEmail(email.trim().toLowerCase());
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setAtivo(true);

        Usuario salvo = usuarioRepository.save(usuario);
        atribuirPapeis(salvo, igrejaId, papeisResolvidos);
        return toResponse(salvo);
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

    /**
     * Papéis de sistema seguem as regras do atalho (MEMBRO, VOLUNTARIO, LIDER);
     * papéis customizados da própria igreja (ex.: CONTADOR) também podem ser atribuídos.
     */
    private Set<String> validarPapeis(Set<String> nomes, UUID igrejaId) {
        if (nomes == null || nomes.isEmpty()) {
            return autorizacaoService.validarPapeisAtalho(nomes);
        }
        Set<String> sistema = new HashSet<>();
        Set<String> customizados = new HashSet<>();
        for (String nome : nomes) {
            String normalizado = nome.trim().toUpperCase();
            if (papelRepository.existsByNomeAndIgrejaId(normalizado, igrejaId)) {
                customizados.add(normalizado);
            } else {
                sistema.add(normalizado);
            }
        }
        Set<String> validados = sistema.isEmpty()
                ? new HashSet<>()
                : new HashSet<>(autorizacaoService.validarPapeisAtalho(sistema));
        validados.addAll(customizados);
        return validados;
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
        Set<String> papeis = usuario.nomesPapeisDaIgrejaAtual();

        String pessoaNome = usuario.getPessoaId() == null
                ? null
                : pessoaService.buscarEntidade(usuario.getPessoaId()).getNome();
        String igrejaNome = usuario.getIgrejaId() == null
                ? null
                : igrejaRepository.findById(usuario.getIgrejaId()).map(Igreja::getNome).orElse(null);

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getPessoaId(),
                pessoaNome,
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getIgrejaId(),
                igrejaNome,
                usuario.getAtivo(),
                papeis,
                usuario.getCreatedAt(),
                usuario.getUpdatedAt()
        );
    }
}
