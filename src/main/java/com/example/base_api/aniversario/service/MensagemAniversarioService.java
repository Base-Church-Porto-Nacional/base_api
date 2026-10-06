package com.example.base_api.aniversario.service;

import com.example.base_api.aniversario.config.MensagemAniversarioProperties;
import com.example.base_api.aniversario.dto.MensagemAniversarioRequest;
import com.example.base_api.aniversario.dto.MensagemAniversarioResponse;
import com.example.base_api.aniversario.entity.MensagemAniversario;
import com.example.base_api.aniversario.repository.MensagemAniversarioRepository;
import com.example.base_api.audit.service.AuditService;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.security.AutorizacaoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Mensagem padrão de aniversário por igreja. A resolução do texto efetivo é:
 * mensagem própria da igreja → mensagem da matriz (para filiais) → template de
 * configuração ({@code app.aniversario.template}).
 */
@Service
public class MensagemAniversarioService {

    private final MensagemAniversarioRepository repository;
    private final IgrejaRepository igrejaRepository;
    private final MensagemAniversarioProperties properties;
    private final AutorizacaoService autorizacaoService;
    private final AuditService auditService;

    public MensagemAniversarioService(
            MensagemAniversarioRepository repository,
            IgrejaRepository igrejaRepository,
            MensagemAniversarioProperties properties,
            AutorizacaoService autorizacaoService,
            AuditService auditService
    ) {
        this.repository = repository;
        this.igrejaRepository = igrejaRepository;
        this.properties = properties;
        this.autorizacaoService = autorizacaoService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public MensagemAniversarioResponse obter(UUID igrejaId) {
        UUID alvo = igrejaId != null ? igrejaId : autorizacaoService.igrejaAtualId();
        autorizacaoService.assertAcesso(alvo);
        return repository.findByIgrejaId(alvo)
                .map(MensagemAniversarioService::toResponse)
                .orElseGet(() -> new MensagemAniversarioResponse(null, alvo, textoParaIgreja(alvo)));
    }

    @Transactional
    public MensagemAniversarioResponse salvar(MensagemAniversarioRequest request) {
        UUID alvo = request.igrejaId() != null ? request.igrejaId() : autorizacaoService.igrejaAtualId();
        autorizacaoService.assertPastorOuAdmin(alvo);
        MensagemAniversario mensagem = repository.findByIgrejaId(alvo).orElseGet(() -> {
            MensagemAniversario nova = new MensagemAniversario();
            nova.setIgrejaId(alvo);
            return nova;
        });
        mensagem.setMensagem(request.mensagem().trim());
        MensagemAniversario salva = repository.save(mensagem);
        auditService.registrar("ALTERAR_MENSAGEM_ANIVERSARIO", "mensagem_aniversario",
                "igrejaId=" + alvo);
        return toResponse(salva);
    }

    /**
     * Texto efetivo usado no envio. Sem verificação de acesso: chamado pela
     * automação, que não tem usuário autenticado.
     */
    @Transactional(readOnly = true)
    public String textoParaIgreja(UUID igrejaId) {
        if (igrejaId == null) {
            return properties.getTemplate();
        }
        return propria(igrejaId)
                .or(() -> igrejaRepository.findById(igrejaId)
                        .map(Igreja::getIgrejaMatrizId)
                        .flatMap(this::propria))
                .orElseGet(properties::getTemplate);
    }

    private Optional<String> propria(UUID igrejaId) {
        return repository.findByIgrejaId(igrejaId)
                .map(MensagemAniversario::getMensagem)
                .filter(t -> !t.isBlank());
    }

    private static MensagemAniversarioResponse toResponse(MensagemAniversario mensagem) {
        return new MensagemAniversarioResponse(mensagem.getId(), mensagem.getIgrejaId(), mensagem.getMensagem());
    }
}
