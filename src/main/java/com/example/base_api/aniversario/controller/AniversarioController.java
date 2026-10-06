package com.example.base_api.aniversario.controller;

import com.example.base_api.aniversario.dto.AniversarioEnvioResponse;
import com.example.base_api.aniversario.dto.AniversarioPessoaResponse;
import com.example.base_api.aniversario.entity.AniversarioEnvio;
import com.example.base_api.aniversario.entity.StatusEnvioAniversario;
import com.example.base_api.aniversario.service.AniversarioService;
import com.example.base_api.aniversario.service.MensagemAniversarioService;
import com.example.base_api.aniversario.service.MensagemAniversarioTemplate;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/aniversarios")
@Tag(name = "Aniversários")
@SecurityRequirement(name = "bearerAuth")
public class AniversarioController {

    private final AniversarioService aniversarioService;
    private final PessoaRepository pessoaRepository;
    private final IgrejaRepository igrejaRepository;
    private final MensagemAniversarioTemplate template;
    private final MensagemAniversarioService mensagemService;
    private final AutorizacaoService autorizacaoService;

    public AniversarioController(
            AniversarioService aniversarioService,
            PessoaRepository pessoaRepository,
            IgrejaRepository igrejaRepository,
            MensagemAniversarioTemplate template,
            MensagemAniversarioService mensagemService,
            AutorizacaoService autorizacaoService
    ) {
        this.aniversarioService = aniversarioService;
        this.pessoaRepository = pessoaRepository;
        this.igrejaRepository = igrejaRepository;
        this.template = template;
        this.mensagemService = mensagemService;
        this.autorizacaoService = autorizacaoService;
    }

    @GetMapping("/aniversariantes")
    @Operation(summary = "Listar os aniversariantes de uma data, com a mensagem e se já foi enviada")
    public List<AniversarioPessoaResponse> listarAniversariantes(
            @RequestParam(required = false) UUID igrejaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId);
        if (ids.isEmpty()) {
            return List.of();
        }
        for (UUID id : ids) {
            autorizacaoService.assertAcesso(id);
        }
        LocalDate referencia = data != null ? data : LocalDate.now();
        List<Pessoa> pessoas = pessoaRepository.findAniversariantesDoDia(
                referencia.getDayOfMonth(), referencia.getMonthValue());
        List<AniversarioEnvio> envios = aniversarioService.listarEnvios(ids, referencia, referencia);

        return pessoas.stream()
                .filter(p -> p.getIgrejaId() != null && ids.contains(p.getIgrejaId()))
                .map(p -> {
                    String mensagem = template.renderizar(p, mensagemService.textoParaIgreja(p.getIgrejaId()), nomeIgreja(p.getIgrejaId()), referencia);
                    boolean enviado = envios.stream().anyMatch(e ->
                            p.getId().equals(e.getPessoaId())
                                    && e.getStatus() == StatusEnvioAniversario.ENVIADO);
                    return new AniversarioPessoaResponse(
                            p.getId(), p.getNome(), p.getDataNascimento(),
                            AniversarioPessoaResponse.idade(p.getDataNascimento(), referencia),
                            mensagem, enviado
                    );
                })
                .toList();
    }

    @GetMapping("/envios")
    @Operation(summary = "Listar o histórico de envios de mensagem de aniversário")
    public List<AniversarioEnvioResponse> listarEnvios(
            @RequestParam(required = false) UUID igrejaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate
    ) {
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId);
        if (ids.isEmpty()) {
            return List.of();
        }
        for (UUID id : ids) {
            autorizacaoService.assertPastorOuAdmin(id);
        }
        LocalDate inicio = de != null ? de : LocalDate.now().minusMonths(1);
        LocalDate fim = ate != null ? ate : LocalDate.now();
        return aniversarioService.listarEnvios(ids, inicio, fim).stream()
                .map(AniversarioEnvioResponse::from)
                .toList();
    }

    @PostMapping("/enviar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Processar e enviar a mensagem de aniversário dos aniversariantes de hoje (idempotente)")
    public String enviarHoje(@RequestParam(required = false) UUID igrejaId) {
        return aniversarioService.enviarAniversariantesDoDiaManual(igrejaId);
    }

    private String nomeIgreja(UUID igrejaId) {
        return igrejaRepository.findById(igrejaId).map(i -> i.getNome()).orElse(null);
    }
}