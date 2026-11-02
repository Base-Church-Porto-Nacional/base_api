package com.example.base_api.usuario.service;

import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.notificacao.CanalMensagem;
import com.example.base_api.notificacao.CanalMensagem.EnvioMensagem;
import com.example.base_api.pessoa.entity.Pessoa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Textos e envio por WhatsApp das mensagens de convite e redefinição de senha. */
@Component
public class MensagemAcesso {

    private static final Logger log = LoggerFactory.getLogger(MensagemAcesso.class);

    private final IgrejaRepository igrejaRepository;
    private final CanalMensagem canalMensagem;

    public MensagemAcesso(IgrejaRepository igrejaRepository, CanalMensagem canalMensagem) {
        this.igrejaRepository = igrejaRepository;
        this.canalMensagem = canalMensagem;
    }

    public String convite(Pessoa pessoa, String link) {
        return "Olá, " + primeiroNome(pessoa) + "! Você foi convidado para usar o app da "
                + nomeIgreja(pessoa.getIgrejaId()) + ". Crie sua senha neste link (vale por 7 dias): " + link;
    }

    public String redefinicao(Pessoa pessoa, String link) {
        return "Olá, " + primeiroNome(pessoa) + "! Para criar uma nova senha do app da "
                + nomeIgreja(pessoa.getIgrejaId()) + ", use este link (vale por 1 hora): " + link;
    }

    /** Falha do canal nunca propaga: o link continua válido. */
    public boolean enviarWhatsapp(Pessoa pessoa, String telefone, String mensagem) {
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

    public String primeiroNome(Pessoa pessoa) {
        String base = pessoa.getNomeSocial() != null && !pessoa.getNomeSocial().isBlank()
                ? pessoa.getNomeSocial()
                : pessoa.getNome();
        return base == null || base.isBlank() ? "" : base.trim().split("\\s+")[0];
    }

    private String nomeIgreja(UUID igrejaId) {
        return igrejaId == null ? "igreja" : igrejaRepository.findById(igrejaId).map(Igreja::getNome).orElse("igreja");
    }
}
