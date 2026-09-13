package com.example.base_api.culto.controller;

import com.example.base_api.culto.dto.CultoConteudoResponse;
import com.example.base_api.culto.dto.CultoConteudoUpdateRequest;
import com.example.base_api.culto.dto.CultoCreateRequest;
import com.example.base_api.culto.dto.CultoRelatorioRequest;
import com.example.base_api.culto.dto.CultoRelatorioResponse;
import com.example.base_api.culto.dto.CultoResponse;
import com.example.base_api.culto.dto.CultoUpdateRequest;
import com.example.base_api.culto.entity.StatusCulto;
import com.example.base_api.culto.service.CultoService;
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
@RequestMapping("/api/cultos")
@Tag(name = "Gestão de Cultos — Cultos")
@SecurityRequirement(name = "bearerAuth")
public class CultoController {

    private final CultoService cultoService;

    public CultoController(CultoService cultoService) {
        this.cultoService = cultoService;
    }

    @GetMapping
    @Operation(summary = "Listar cultos (filtros: igreja, categoria, competência/mês, status, período)")
    public List<CultoResponse> listar(
            @RequestParam(required = false) UUID igrejaId,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) StatusCulto status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Boolean ativo
    ) {
        return cultoService.listar(igrejaId, categoriaId, competencia, status, inicio, fim, ativo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar culto")
    public CultoResponse buscar(@PathVariable UUID id) {
        return cultoService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Criar culto manualmente vinculado a uma categoria")
    public CultoResponse criar(@Valid @RequestBody CultoCreateRequest request) {
        return cultoService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualizar dados e status do culto")
    public CultoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody CultoUpdateRequest request) {
        return cultoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Excluir culto (bloqueado quando já possui relatório)")
    public void excluir(@PathVariable UUID id) {
        cultoService.excluir(id);
    }

    @GetMapping("/{id}/conteudo")
    @Operation(summary = "Obter conteúdo do culto (louvor, pregação, tema e pregador)")
    public CultoConteudoResponse obterConteudo(@PathVariable UUID id) {
        return cultoService.obterConteudo(id);
    }

    @PutMapping("/{id}/conteudo")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Salvar conteúdo do culto. Texto: pastor/pregador. Louvor: ministério de música.")
    public CultoConteudoResponse salvarConteudo(
            @PathVariable UUID id,
            @RequestBody CultoConteudoUpdateRequest request
    ) {
        return cultoService.salvarConteudo(id, request);
    }

    @GetMapping("/{id}/relatorio")
    @Operation(summary = "Obter relatório do culto (participantes e veículos)")
    public CultoRelatorioResponse obterRelatorio(@PathVariable UUID id) {
        return cultoService.obterRelatorio(id);
    }

    @PutMapping("/{id}/relatorio")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Salvar relatório do culto. O culto passa automaticamente para REALIZADO.")
    public CultoRelatorioResponse salvarRelatorio(
            @PathVariable UUID id,
            @Valid @RequestBody CultoRelatorioRequest request
    ) {
        return cultoService.salvarRelatorio(id, request);
    }
}