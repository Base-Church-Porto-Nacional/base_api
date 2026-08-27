package com.example.base_api.cronograma.controller;

import com.example.base_api.cronograma.dto.CronogramaConteudoResponse;
import com.example.base_api.cronograma.dto.CronogramaConteudoUpdateRequest;
import com.example.base_api.cronograma.dto.CronogramaEventoCreateRequest;
import com.example.base_api.cronograma.dto.CronogramaEventoResponse;
import com.example.base_api.cronograma.dto.CronogramaEventoUpdateRequest;
import com.example.base_api.cronograma.service.CronogramaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cronograma")
@Tag(name = "Cronograma")
@SecurityRequirement(name = "bearerAuth")
public class CronogramaController {

    private final CronogramaService cronogramaService;

    public CronogramaController(CronogramaService cronogramaService) {
        this.cronogramaService = cronogramaService;
    }

    @GetMapping
    @Operation(summary = "Listar eventos do cronograma (opcional: filtro por período)")
    public List<CronogramaEventoResponse> listar(
            @RequestParam(required = false) UUID igrejaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return cronogramaService.listar(igrejaId, inicio, fim);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar evento do cronograma")
    public CronogramaEventoResponse buscar(@PathVariable UUID id) {
        return cronogramaService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Criar culto/evento no cronograma")
    public CronogramaEventoResponse criar(@Valid @RequestBody CronogramaEventoCreateRequest request) {
        return cronogramaService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualizar evento do cronograma")
    public CronogramaEventoResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody CronogramaEventoUpdateRequest request
    ) {
        return cronogramaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Excluir evento do cronograma")
    public void excluir(@PathVariable UUID id) {
        cronogramaService.excluir(id);
    }

    @GetMapping("/{id}/conteudo")
    @Operation(summary = "Obter conteúdo do culto (texto da pregação e louvor)")
    public CronogramaConteudoResponse obterConteudo(@PathVariable UUID id) {
        return cronogramaService.obterConteudo(id);
    }

    @PutMapping("/{id}/conteudo")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Salvar conteúdo do culto. Texto: pastor/pregador. Louvor: ministério de música. Mídia: somente leitura.")
    public CronogramaConteudoResponse salvarConteudo(
            @PathVariable UUID id,
            @RequestBody CronogramaConteudoUpdateRequest request
    ) {
        return cronogramaService.salvarConteudo(id, request);
    }
}
