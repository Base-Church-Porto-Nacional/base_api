package com.example.base_api.usuario.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.common.exception.EntidadeNaoProcessavelException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.notificacao.CanalMensagem;
import com.example.base_api.notificacao.CanalMensagem.EnvioMensagem;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Convite de pessoas ao app e geração de link de redefinição de senha por quem administra. */
@Service
public class AcessoService {

    private static final Logger log = LoggerFactory.getLogger(AcessoService.class);

    private static final String CHAVE_TOKEN = "token";
    private static final String MENSAGEM_LINK_EXPIRADO = TokenAcessoService.MENSAGEM_LINK_EXPIRADO;

    private final PessoaService pessoaService;
    private final UsuarioRepository usuarioRepository;
    private final IgrejaRepository igrejaRepository;
    private final TokenAcessoService tokenAcessoService;
    private final CanalMensagem canalMensagem;
    private final AutorizacaoService autorizacaoService;
    private final UsuarioService usuarioService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter loginRateLimiter;

    public AcessoService(
            PessoaService pessoaService,
            UsuarioRepository usuarioRepository,
            IgrejaRepository igrejaRepository,
            TokenAcessoService tokenAcessoService,
            CanalMensagem canalMensagem,
            AutorizacaoService autorizacaoService,
            UsuarioService usuarioService,
            AuditService auditService,
            PasswordEncoder passwordEncoder,
            LoginRateLimiter loginRateLimiter
    ) {
        this.pessoaService = pessoaService;
        this.usuarioRepository = usuarioRepository;
        this.igrejaRepository = igrejaRepository;
        this.tokenAcessoService = tokenAcessoService;
        this.canalMensagem = canalMensagem;
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
        String mensagem = "Olá, " + primeiroNome(pessoa) + "! Você foi convidado para usar o app da "
                + nomeIgreja(pessoa.getIgrejaId()) + ". Crie sua senha neste link (vale por 7 dias): " + link;
        boolean enviado = enviarWhatsapp(pessoa, telefone.orElse(null), mensagem);

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
        String mensagem = mensagemRedefinicao(pessoa, link);
        boolean enviado = enviarWhatsapp(pessoa, usuario.getTelefone(), mensagem);

        auditService.registrar("GERAR_REDEFINICAO", "usuario", "usuarioId=" + usuarioId);
        return new LinkAcessoResponse(link, enviado);
    }


    private String mensagemRedefinicao(Pessoa pessoa, String link) {
        return "Olá, " + primeiroNome(pessoa) + "! Para criar uma nova senha do app da "
                + nomeIgreja(pessoa.getIgrejaId()) + ", use este link (vale por 1 hora): " + link;
    }

    /** Informa tipo e primeiro nome de um link válido; link inválido conta como falha no rate limit. */
    @Transactional(readOnly = true)
    public TokenInfoResponse info(String token, String ip) {
        loginRateLimiter.assertPermitido(CHAVE_TOKEN, ip);
        try {
            TokenAcesso t = tokenAcessoService.validar(token);
            Usuario usuario = usuarioRepository.findById(t.getUsuarioId())
                    .filter(u -> u.getStatus() != StatusUsuario.DESATIVADO)
                    .orElseThrow(() -> new LinkExpiradoException(MENSAGEM_LINK_EXPIRADO));
            Pessoa pessoa = pessoaService.buscarEntidade(usuario.getPessoaId());
            return new TokenInfoResponse(t.getTipo(), primeiroNome(pessoa));
        } catch (LinkExpiradoException e) {
            loginRateLimiter.registrarFalha(CHAVE_TOKEN, ip);
            throw e;
        }
    }

    @Transactional
    public LoginResponse definirSenha(DefinirSenhaRequest request, String ip) {
        loginRateLimiter.assertPermitido(CHAVE_TOKEN, ip);
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
            loginRateLimiter.registrarFalha(CHAVE_TOKEN, ip);
            throw e;
        }
    }

    /**
     * Esqueci a senha: nunca revela se a conta existe. Só o rate limit pode falhar a chamada;
     * conta inexistente, convidada, desativada ou sem telefone resulta em silêncio.
     */
    @Transactional
    public void solicitarRedefinicao(String identificador, String ip) {
        String normalizado = Identificador.normalizado(identificador);
        String chave = "esqueci:" + (normalizado == null || normalizado.isBlank()
                ? (identificador == null ? "" : identificador.trim())
                : normalizado);
        loginRateLimiter.assertPermitido(chave, ip);
        loginRateLimiter.registrarFalha(chave, ip);

        Usuario usuario = usuarioRepository.findByIdentificador(identificador).orElse(null);
        if (usuario == null || usuario.getStatus() != StatusUsuario.ATIVO
                || usuario.getTelefone() == null || usuario.getPessoaId() == null) {
            return;
        }
        try {
            Pessoa pessoa = pessoaService.buscarEntidade(usuario.getPessoaId());
            String link = tokenAcessoService.link(
                    tokenAcessoService.gerar(usuario.getId(), TipoTokenAcesso.REDEFINICAO, null));
            enviarWhatsapp(pessoa, usuario.getTelefone(), mensagemRedefinicao(pessoa, link));
        } catch (Exception e) {
            log.warn("Falha ao processar esqueci a senha (usuarioId={}): {}", usuario.getId(), e.getMessage());
        }
    }

    /** Falha do canal nunca propaga: o link continua válido e é devolvido a quem administra. */
    private boolean enviarWhatsapp(Pessoa pessoa, String telefone, String mensagem) {
        if (telefone == null) {
            return false;
        }
        try {
            canalMensagem.enviar(new EnvioMensagem(pessoa.getId(), pessoa.getNome(), telefone, pessoa.getEmail(), mensagem));
            return true;
        } catch (Exception e) {
            log.warn("Não foi possível enviar o link de acesso por WhatsApp (pessoaId={}): {}", pessoa.getId(), e.getMessage());
            return false;
        }
    }

    private static String primeiroNome(Pessoa pessoa) {
        String base = pessoa.getNomeSocial() != null && !pessoa.getNomeSocial().isBlank()
                ? pessoa.getNomeSocial()
                : pessoa.getNome();
        return base == null ? "" : base.trim().split("\\s+")[0];
    }

    private String nomeIgreja(UUID igrejaId) {
        return igrejaRepository.findById(igrejaId).map(Igreja::getNome).orElse("igreja");
    }
}
