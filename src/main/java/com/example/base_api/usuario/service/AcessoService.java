package com.example.base_api.usuario.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.common.exception.EntidadeNaoProcessavelException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.security.Capacidade;
import com.example.base_api.security.Identificador;
import com.example.base_api.common.exception.LinkExpiradoException;
import com.example.base_api.security.LoginRateLimiter;
import com.example.base_api.usuario.dto.DefinirSenhaRequest;
import com.example.base_api.usuario.dto.LinkAcessoResponse;
import com.example.base_api.usuario.dto.LoginResponse;
import com.example.base_api.usuario.dto.TokenInfoResponse;
import com.example.base_api.usuario.entity.TokenAcesso;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.base_api.usuario.entity.StatusUsuario;
import com.example.base_api.usuario.entity.TipoTokenAcesso;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

/** Convite de pessoas ao app e geração de link de redefinição de senha por quem administra. */
@Service
public class AcessoService {

    private static final Logger log = LoggerFactory.getLogger(AcessoService.class);

    private static final String CHAVE_TOKEN = "token";
    private static final String ESCOPO_TOKEN = "token";
    private static final String ESCOPO_ESQUECI = "esqueci";
    private static final String MENSAGEM_LINK_EXPIRADO = TokenAcessoService.MENSAGEM_LINK_EXPIRADO;

    private final PessoaService pessoaService;
    private final UsuarioRepository usuarioRepository;
    private final TokenAcessoService tokenAcessoService;
    private final MensagemAcesso mensagemAcesso;
    private final EnvioRedefinicaoService envioRedefinicaoService;
    private final Executor acessoExecutor;
    private final AutorizacaoService autorizacaoService;
    private final UsuarioService usuarioService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter loginRateLimiter;

    public AcessoService(
            PessoaService pessoaService,
            UsuarioRepository usuarioRepository,
            TokenAcessoService tokenAcessoService,
            MensagemAcesso mensagemAcesso,
            EnvioRedefinicaoService envioRedefinicaoService,
            @Qualifier("acessoExecutor") Executor acessoExecutor,
            AutorizacaoService autorizacaoService,
            UsuarioService usuarioService,
            AuditService auditService,
            PasswordEncoder passwordEncoder,
            LoginRateLimiter loginRateLimiter
    ) {
        this.pessoaService = pessoaService;
        this.usuarioRepository = usuarioRepository;
        this.tokenAcessoService = tokenAcessoService;
        this.mensagemAcesso = mensagemAcesso;
        this.envioRedefinicaoService = envioRedefinicaoService;
        this.acessoExecutor = acessoExecutor;
        this.autorizacaoService = autorizacaoService;
        this.usuarioService = usuarioService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Transactional
    public LinkAcessoResponse convidar(UUID pessoaId) {
        Pessoa pessoa = pessoaService.buscarEntidade(pessoaId);
        autorizacaoService.assertPodeConvidar(pessoa);

        Optional<String> email = Identificador.email(pessoa.getEmail());
        Optional<String> telefone = Identificador.telefone(pessoa.getWhatsapp())
                .or(() -> Identificador.telefone(pessoa.getTelefone()));
        if (email.isEmpty() && telefone.isEmpty()) {
            throw new EntidadeNaoProcessavelException("Cadastre e-mail ou WhatsApp antes de convidar");
        }

        Usuario usuario = usuarioRepository.findByPessoaId(pessoaId).orElse(null);
        if (usuario == null || usuario.getStatus() == StatusUsuario.CONVIDADO) {
            UUID propriaContaId = usuario == null ? null : usuario.getId();
            email.flatMap(usuarioRepository::findByEmailIgnoreCase)
                    .filter(outro -> !java.util.Objects.equals(outro.getId(), propriaContaId))
                    .ifPresent(outro -> {
                        throw new ConflictException("E-mail já usado por outra conta");
                    });
            if (telefone.isPresent()) {
                boolean outraConta = propriaContaId == null
                        ? usuarioRepository.existsByTelefone(telefone.get())
                        : usuarioRepository.findByTelefone(telefone.get())
                                .filter(outro -> !java.util.Objects.equals(outro.getId(), propriaContaId)).isPresent();
                if (outraConta) {
                    throw new ConflictException("Telefone já usado por outra conta");
                }
            }
        }
        if (usuario == null) {
            usuario = new Usuario();
            usuario.setPessoaId(pessoaId);
            usuario.setIgrejaId(pessoa.getIgrejaId());
            usuario.setEmail(email.orElse(null));
            usuario.setTelefone(telefone.orElse(null));
            usuario.setStatus(StatusUsuario.CONVIDADO);
            usuario = usuarioRepository.save(usuario);
        } else if (usuario.getStatus() == StatusUsuario.CONVIDADO) {
            usuario.setEmail(email.orElse(null));
            usuario.setTelefone(telefone.orElse(null));
            usuario = usuarioRepository.save(usuario);
        } else if (usuario.getStatus() == StatusUsuario.ATIVO) {
            throw new ConflictException("Esta pessoa já tem acesso ao app");
        } else if (usuario.getStatus() == StatusUsuario.DESATIVADO) {
            throw new ConflictException("A conta está desativada. Reative antes de convidar");
        }

        UUID atorId = autorizacaoService.requireUsuario().getId();
        String link = tokenAcessoService.link(tokenAcessoService.gerar(usuario.getId(), TipoTokenAcesso.CONVITE, atorId));
        String mensagem = mensagemAcesso.convite(pessoa, link);
        boolean enviado = mensagemAcesso.enviarWhatsapp(pessoa, telefone.orElse(null), mensagem);

        auditService.registrar("CONVIDAR", "usuario", "pessoaId=" + pessoaId + " usuarioId=" + usuario.getId());
        return new LinkAcessoResponse(link, enviado);
    }

    @Transactional
    public LinkAcessoResponse gerarRedefinicao(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        autorizacaoService.assertTem(usuario.getIgrejaId(), Capacidade.CONTAS_GERIR);
        usuarioService.assertPodeAlterarContaDeGestao(usuario);
        if (usuario.getStatus() != StatusUsuario.ATIVO) {
            throw new ConflictException("Só é possível redefinir a senha de uma conta ativa");
        }

        Pessoa pessoa = pessoaService.buscarEntidade(usuario.getPessoaId());
        UUID atorId = autorizacaoService.requireUsuario().getId();
        String link = tokenAcessoService.link(tokenAcessoService.gerar(usuarioId, TipoTokenAcesso.REDEFINICAO, atorId));
        String mensagem = mensagemAcesso.redefinicao(pessoa, link);
        boolean enviado = mensagemAcesso.enviarWhatsapp(pessoa, usuario.getTelefone(), mensagem);

        auditService.registrar("GERAR_REDEFINICAO", "usuario", "usuarioId=" + usuarioId);
        return new LinkAcessoResponse(link, enviado);
    }


    /** Informa tipo e primeiro nome de um link válido; link inválido conta como falha no rate limit. */
    @Transactional(readOnly = true)
    public TokenInfoResponse info(String token, String ip) {
        loginRateLimiter.assertPermitido(ESCOPO_TOKEN, CHAVE_TOKEN, ip);
        try {
            TokenAcesso t = tokenAcessoService.validar(token);
            Usuario usuario = usuarioRepository.findById(t.getUsuarioId())
                    .filter(u -> u.getStatus() != StatusUsuario.DESATIVADO)
                    .orElseThrow(() -> new LinkExpiradoException(MENSAGEM_LINK_EXPIRADO));
            Pessoa pessoa;
            try {
                pessoa = pessoaService.buscarEntidade(usuario.getPessoaId());
            } catch (ResourceNotFoundException e) {
                throw new LinkExpiradoException(MENSAGEM_LINK_EXPIRADO);
            }
            return new TokenInfoResponse(t.getTipo(), mensagemAcesso.primeiroNome(pessoa));
        } catch (LinkExpiradoException e) {
            loginRateLimiter.registrarFalha(ESCOPO_TOKEN, CHAVE_TOKEN, ip);
            throw e;
        }
    }

    @Transactional
    public LoginResponse definirSenha(DefinirSenhaRequest request, String ip) {
        loginRateLimiter.assertPermitido(ESCOPO_TOKEN, CHAVE_TOKEN, ip);
        try {
            TokenAcesso t = tokenAcessoService.validar(request.token());
            Usuario usuario = usuarioRepository.findById(t.getUsuarioId())
                    .filter(u -> u.getStatus() != StatusUsuario.DESATIVADO)
                    .orElseThrow(() -> new LinkExpiradoException(MENSAGEM_LINK_EXPIRADO));
            tokenAcessoService.consumir(t);

            usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
            usuario.setStatus(StatusUsuario.ATIVO);
            usuario.invalidarTokens();
            tokenAcessoService.revogarPendentes(usuario.getId());
            usuario = usuarioRepository.save(usuario);

            auditService.registrar("DEFINIR_SENHA", "usuario", "usuarioId=" + usuario.getId() + " tipo=" + t.getTipo());
            return usuarioService.emitirLogin(usuario);
        } catch (LinkExpiradoException e) {
            loginRateLimiter.registrarFalha(ESCOPO_TOKEN, CHAVE_TOKEN, ip);
            throw e;
        }
    }

    /**
     * Esqueci a senha: nunca revela se a conta existe. A thread da requisição só aplica o rate limit e faz
     * a consulta; geração do token e envio rodam depois do commit, em executor próprio, para que nem o
     * tempo de resposta denuncie a conta. Só o rate limit pode falhar a chamada.
     */
    @Transactional(readOnly = true)
    public void solicitarRedefinicao(String identificador, String ip) {
        String normalizado = Identificador.normalizado(identificador);
        String chave = normalizado.isBlank() ? (identificador == null ? "" : identificador.trim()) : normalizado;
        loginRateLimiter.assertPermitido(ESCOPO_ESQUECI, chave, ip);
        loginRateLimiter.registrarFalha(ESCOPO_ESQUECI, chave, ip);

        try {
            Usuario usuario = usuarioRepository.findByIdentificador(identificador).orElse(null);
            if (usuario == null || usuario.getStatus() != StatusUsuario.ATIVO
                    || usuario.getTelefone() == null || usuario.getPessoaId() == null) {
                return;
            }
            UUID usuarioId = usuario.getId();
            Runnable envio = () -> acessoExecutor.execute(() -> envioRedefinicaoService.gerarEEnviar(usuarioId));
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        envio.run();
                    }
                });
            } else {
                envio.run();
            }
        } catch (Exception e) {
            log.warn("Falha ao agendar esqueci a senha: {}", e.getClass().getSimpleName());
        }
    }
}
