package com.example.base_api.audit.service;

import com.example.base_api.audit.entity.AuditLog;
import com.example.base_api.audit.repository.AuditLogRepository;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditService(AuditLogRepository auditLogRepository, UsuarioRepository usuarioRepository) {
        this.auditLogRepository = auditLogRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String acao, String recurso, String detalhes) {
        AuditLog evento = new AuditLog();
        Usuario usuario = usuarioAtual();
        if (usuario != null) {
            evento.setUsuarioId(usuario.getId());
            evento.setIgrejaId(usuario.getIgrejaId());
        }
        evento.setAcao(acao);
        evento.setRecurso(recurso);
        evento.setDetalhes(detalhes);
        auditLogRepository.save(evento);
        log.info("audit acao={} recurso={} usuario={} detalhes={}", acao, recurso,
                usuario == null ? null : usuario.getUsername(), detalhes);
    }

    private Usuario usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return usuarioRepository.findByUsername(authentication.getName()).orElse(null);
    }
}
