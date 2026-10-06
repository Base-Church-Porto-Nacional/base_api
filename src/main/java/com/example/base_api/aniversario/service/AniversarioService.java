package com.example.base_api.aniversario.service;

import com.example.base_api.aniversario.config.MensagemAniversarioProperties;
import com.example.base_api.aniversario.entity.AniversarioEnvio;
import com.example.base_api.aniversario.repository.AniversarioEnvioRepository;
import com.example.base_api.audit.service.AuditService;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Automação de aniversário: identifica os membros cujo aniversário (dia e mês da
 * data de nascimento) é o dia da execução, envia a mensagem padrão personalizada
 * e registra cada envio com seu status. O envio é idempotente por
 * (pessoa, data do aniversário): um membro nunca recebe duas mensagens no mesmo
 * aniversário. Membros sem data de nascimento são ignorados.
 */
@Service
public class AniversarioService {

    private static final Logger log = LoggerFactory.getLogger(AniversarioService.class);

    private final PessoaRepository pessoaRepository;
    private final AniversarioEnvioRepository envioRepository;
    private final AniversarioEnvioService envioService;
    private final MensagemAniversarioProperties properties;
    private final AutorizacaoService autorizacaoService;
    private final AuditService auditService;

    public AniversarioService(
            PessoaRepository pessoaRepository,
            AniversarioEnvioRepository envioRepository,
            AniversarioEnvioService envioService,
            MensagemAniversarioProperties properties,
            AutorizacaoService autorizacaoService,
            AuditService auditService
    ) {
        this.pessoaRepository = pessoaRepository;
        this.envioRepository = envioRepository;
        this.envioService = envioService;
        this.properties = properties;
        this.autorizacaoService = autorizacaoService;
        this.auditService = auditService;
    }

    @Scheduled(cron = "${app.aniversario.cron:0 0 12 * * *}", zone = "${app.timezone:America/Sao_Paulo}")
    public String enviarAniversariantesDoDia() {
        if (!properties.isAtivo()) {
            String msg = "Mensagem de aniversário desativada por configuração";
            log.info(msg);
            return msg;
        }
        return processar(LocalDate.now());
    }

    /**
     * Execução manual para as igrejas visíveis ao usuário autenticado.
     */
    @Transactional
    public String enviarAniversariantesDoDiaManual(UUID igrejaId) {
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId);
        if (ids.isEmpty()) {
            return "Nenhuma igreja visível para enviar mensagem de aniversário";
        }
        for (UUID id : ids) {
            autorizacaoService.assertPastorOuAdmin(id);
        }
        LocalDate hoje = LocalDate.now();
        // Restringe às igrejas autorizadas: processar() sozinho varre a rede inteira.
        String resumo = processar(hoje, ids);
        auditService.registrar("ENVIAR_ANIVERSARIO", "pessoa",
                "data=" + hoje + " igrejaIds=" + ids + " resumo=" + resumo);
        return resumo;
    }

    /**
     * Processa os aniversariantes de uma data em toda a rede. Usado pela
     * automação, que não tem usuário autenticado.
     */
    public String processar(LocalDate dataAniversario) {
        return processar(dataAniversario, null);
    }

    /**
     * Processa os aniversariantes de uma data: envia a mensagem e registra o
     * resultado de cada membro. Retorna um resumo em português.
     *
     * @param limiteIgrejas quando não nulo, apenas membros dessas igrejas entram
     *                      no envio; nulo significa toda a rede.
     */
    public String processar(LocalDate dataAniversario, List<UUID> limiteIgrejas) {
        List<Pessoa> aniversariantes = pessoaRepository.findAniversariantesDoDia(
                dataAniversario.getDayOfMonth(), dataAniversario.getMonthValue());
        if (limiteIgrejas != null) {
            aniversariantes = aniversariantes.stream()
                    .filter(p -> limiteIgrejas.contains(p.getIgrejaId()))
                    .toList();
        }

        if (aniversariantes.isEmpty()) {
            String msg = "Nenhum aniversariante em " + dataAniversario;
            log.info(msg);
            return msg;
        }

        int enviados = 0;
        int ignorados = 0;
        int falhas = 0;
        int enfileirados = 0;
        for (Pessoa pessoa : aniversariantes) {
            if (pessoa.getDataNascimento() == null) {
                ignorados++;
                continue;
            }
            if (envioRepository.existsByPessoaIdAndDataAniversario(pessoa.getId(), dataAniversario)) {
                ignorados++;
                continue;
            }
            AniversarioEnvioService.Resultado resultado =
                    envioService.enviarPara(pessoa, dataAniversario);
            switch (resultado) {
                case ENVIADO -> enviados++;
                case ENVIADO_PENDENTE -> enfileirados++;
                case FALHA -> falhas++;
            }
        }

        String resumo = "Aniversariantes de " + dataAniversario + ": " + enviados
                + " enviados, " + enfileirados + " enfileirados, " + ignorados
                + " ignorados, " + falhas + " falhas.";
        log.info(resumo);
        return resumo;
    }

    @Transactional(readOnly = true)
    public List<AniversarioEnvio> listarEnvios(List<UUID> igrejaIds, LocalDate inicio, LocalDate fim) {
        if (igrejaIds.isEmpty()) {
            return List.of();
        }
        return envioRepository.findByIgrejaIdInAndDataAniversarioBetweenOrderByDataProcessamentoDesc(
                igrejaIds, inicio, fim);
    }
}