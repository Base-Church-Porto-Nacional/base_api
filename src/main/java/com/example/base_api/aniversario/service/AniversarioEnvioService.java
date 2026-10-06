package com.example.base_api.aniversario.service;

import com.example.base_api.aniversario.config.MensagemAniversarioProperties;
import com.example.base_api.aniversario.entity.AniversarioEnvio;
import com.example.base_api.aniversario.entity.StatusEnvioAniversario;
import com.example.base_api.aniversario.repository.AniversarioEnvioRepository;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.notificacao.CanalMensagem;
import com.example.base_api.notificacao.despacho.DespachadorMensagem;
import com.example.base_api.pessoa.entity.Pessoa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Executa o envio da mensagem de aniversário de um membro e persiste o registro
 * em transação própria (REQUIRES_NEW), para que uma falha de um membro não
 * reverta o processamento dos demais.
 *
 * <p>Com {@code app.rabbit.habilitado=true} o registro fica PENDENTE e o
 * despachante apenas publica o evento — quem entrega e grava ENVIADO/FALHA é o
 * consumidor. Sem Rabbit, o envio é síncrono e o status sai resolvido na hora.</p>
 */
@Service
public class AniversarioEnvioService {

    private static final Logger log = LoggerFactory.getLogger(AniversarioEnvioService.class);

    private final AniversarioEnvioRepository envioRepository;
    private final IgrejaRepository igrejaRepository;
    private final DespachadorMensagem despachador;
    private final MensagemAniversarioTemplate template;
    private final MensagemAniversarioProperties properties;

    public AniversarioEnvioService(
            AniversarioEnvioRepository envioRepository,
            IgrejaRepository igrejaRepository,
            DespachadorMensagem despachador,
            MensagemAniversarioTemplate template,
            MensagemAniversarioProperties properties
    ) {
        this.envioRepository = envioRepository;
        this.igrejaRepository = igrejaRepository;
        this.despachador = despachador;
        this.template = template;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Resultado enviarPara(Pessoa pessoa, LocalDate dataAniversario) {
        String mensagem = template.renderizar(
                pessoa, properties.getTemplate(), nomeIgreja(pessoa.getIgrejaId()), dataAniversario);

        AniversarioEnvio envio = new AniversarioEnvio();
        envio.setPessoaId(pessoa.getId());
        envio.setIgrejaId(pessoa.getIgrejaId());
        envio.setDataAniversario(dataAniversario);
        envio.setDataProcessamento(Instant.now());
        envio.setMensagem(mensagem);
        envio.setStatus(StatusEnvioAniversario.PENDENTE);
        envioRepository.save(envio);

        DespachadorMensagem.Resultado resultado = despachador.despachar(envio, pessoa, mensagem);
        envio.setStatus(resultado.status());
        envio.setErro(resultado.erro());
        envioRepository.save(envio);

        return switch (resultado.status()) {
            case ENVIADO -> Resultado.ENVIADO;
            case PENDENTE -> Resultado.ENVIADO_PENDENTE;
            case FALHA -> {
                log.warn("falha ao despachar mensagem de aniversário para pessoa={}: {}",
                        pessoa.getId(), resultado.erro());
                yield Resultado.FALHA;
            }
        };
    }

    private String nomeIgreja(UUID igrejaId) {
        return igrejaRepository.findById(igrejaId)
                .map(Igreja::getNome)
                .orElse(null);
    }

    public enum Resultado {
        /** Canal aceitou a mensagem (envio síncrono). */
        ENVIADO,
        /** Publicado na fila; desfecho sai quando o consumidor processar. */
        ENVIADO_PENDENTE,
        FALHA
    }
}