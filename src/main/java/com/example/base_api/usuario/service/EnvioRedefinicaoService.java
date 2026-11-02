package com.example.base_api.usuario.service;

import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.usuario.entity.StatusUsuario;
import com.example.base_api.usuario.entity.TipoTokenAcesso;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Gera e envia o link de "esqueci a senha". Roda fora da requisição (após o commit, em executor próprio),
 * em transação própria, para o tempo de resposta não revelar se a conta existe.
 */
@Service
public class EnvioRedefinicaoService {

    private static final Logger log = LoggerFactory.getLogger(EnvioRedefinicaoService.class);

    private final UsuarioRepository usuarioRepository;
    private final PessoaService pessoaService;
    private final TokenAcessoService tokenAcessoService;
    private final MensagemAcesso mensagemAcesso;

    public EnvioRedefinicaoService(
            UsuarioRepository usuarioRepository,
            PessoaService pessoaService,
            TokenAcessoService tokenAcessoService,
            MensagemAcesso mensagemAcesso
    ) {
        this.usuarioRepository = usuarioRepository;
        this.pessoaService = pessoaService;
        this.tokenAcessoService = tokenAcessoService;
        this.mensagemAcesso = mensagemAcesso;
    }

    /** Nunca propaga erro; só o id do usuário vai ao log. */
    @Transactional
    public void gerarEEnviar(UUID usuarioId) {
        try {
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            if (usuario == null || usuario.getStatus() != StatusUsuario.ATIVO
                    || usuario.getTelefone() == null || usuario.getPessoaId() == null) {
                return;
            }
            Pessoa pessoa = pessoaService.buscarEntidade(usuario.getPessoaId());
            String link = tokenAcessoService.link(
                    tokenAcessoService.gerar(usuario.getId(), TipoTokenAcesso.REDEFINICAO, null));
            mensagemAcesso.enviarWhatsapp(pessoa, usuario.getTelefone(), mensagemAcesso.redefinicao(pessoa, link));
        } catch (Exception e) {
            log.warn("Falha ao processar esqueci a senha (usuarioId={})", usuarioId);
        }
    }
}
