package com.example.base_api.aniversario.service;

import com.example.base_api.aniversario.entity.AniversarioEnvio;
import com.example.base_api.aniversario.entity.StatusEnvioAniversario;
import com.example.base_api.aniversario.repository.AniversarioEnvioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Atualiza o desfecho do registro de envio. Usado tanto pelo envio síncrono
 * quanto pelo consumidor do RabbitMQ, para que o status final seja sempre
 * gravado em transação própria.
 */
@Service
public class AniversarioEnvioStatusService {

    private static final Logger log = LoggerFactory.getLogger(AniversarioEnvioStatusService.class);

    private final AniversarioEnvioRepository envioRepository;

    public AniversarioEnvioStatusService(AniversarioEnvioRepository envioRepository) {
        this.envioRepository = envioRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEnviado(UUID envioId) {
        atualizar(envioId, StatusEnvioAniversario.ENVIADO, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFalha(UUID envioId, Throwable causa) {
        String mensagem = causa == null
                ? "erro desconhecido"
                : (causa.getMessage() == null ? causa.getClass().getSimpleName() : causa.getMessage());
        atualizar(envioId, StatusEnvioAniversario.FALHA, mensagem);
    }

    private void atualizar(UUID envioId, StatusEnvioAniversario status, String erro) {
        AniversarioEnvio envio = envioRepository.findById(envioId).orElse(null);
        if (envio == null) {
            log.warn("Registro de envio {} não encontrado para atualizar status {}", envioId, status);
            return;
        }
        if (envio.getStatus() != StatusEnvioAniversario.PENDENTE) {
            return;
        }
        envio.setStatus(status);
        envio.setErro(erro);
        envioRepository.save(envio);
    }
}