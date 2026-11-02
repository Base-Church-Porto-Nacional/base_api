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
import com.example.base_api.usuario.dto.LinkAcessoResponse;
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

    private final PessoaService pessoaService;
    private final UsuarioRepository usuarioRepository;
    private final IgrejaRepository igrejaRepository;
    private final TokenAcessoService tokenAcessoService;
    private final CanalMensagem canalMensagem;
    private final AutorizacaoService autorizacaoService;
    private final UsuarioService usuarioService;
    private final AuditService auditService;

    public AcessoService(
            PessoaService pessoaService,
            UsuarioRepository usuarioRepository,
            IgrejaRepository igrejaRepository,
            TokenAcessoService tokenAcessoService,
            CanalMensagem canalMensagem,
            AutorizacaoService autorizacaoService,
            UsuarioService usuarioService,
            AuditService auditService
    ) {
        this.pessoaService = pessoaService;
        this.usuarioRepository = usuarioRepository;
        this.igrejaRepository = igrejaRepository;
        this.tokenAcessoService = tokenAcessoService;
        this.canalMensagem = canalMensagem;
        this.autorizacaoService = autorizacaoService;
        this.usuarioService = usuarioService;
        this.auditService = auditService;
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
        if (usuario == null) {
            email.flatMap(usuarioRepository::findByEmailIgnoreCase).ifPresent(outro -> {
                throw new ConflictException("E-mail já usado por outra conta");
            });
            if (telefone.isPresent() && usuarioRepository.existsByTelefone(telefone.get())) {
                throw new ConflictException("Telefone já usado por outra conta");
            }
            usuario = new Usuario();
            usuario.setPessoaId(pessoaId);
            usuario.setIgrejaId(pessoa.getIgrejaId());
            usuario.setEmail(email.orElse(null));
            usuario.setTelefone(telefone.orElse(null));
            usuario.setStatus(StatusUsuario.CONVIDADO);
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
        String mensagem = "Olá, " + primeiroNome(pessoa) + "! Para criar uma nova senha do app da "
                + nomeIgreja(pessoa.getIgrejaId()) + ", use este link (vale por 1 hora): " + link;
        boolean enviado = enviarWhatsapp(pessoa, usuario.getTelefone(), mensagem);

        auditService.registrar("GERAR_REDEFINICAO", "usuario", "usuarioId=" + usuarioId);
        return new LinkAcessoResponse(link, enviado);
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
