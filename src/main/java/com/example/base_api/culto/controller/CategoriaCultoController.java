package com.example.base_api.culto.controller;

import com.example.base_api.culto.dto.CategoriaCultoCreateRequest;
import com.example.base_api.culto.dto.CategoriaCultoResponse;
import com.example.base_api.culto.dto.CategoriaCultoUpdateRequest;
import com.example.base_api.culto.service.CategoriaCultoService;
import com.example.base_api.culto.service.CultoAutomacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categorias-culto")
@Tag(name = "Gestão de Cultos — Categorias")
@SecurityRequirement(name = "bearerAuth")
public class CategoriaCultoController {

    private final CategoriaCultoService categoriaService;
    private final CultoAutomacaoService automacaoService;

    public CategoriaCultoController(
            CategoriaCultoService categoriaService,
            CultoAutomacaoService automacaoService
    ) {
        this.categoriaService = categoriaService;
        this.automacaoService = automacaoService;
    }

    @GetMapping
    @Operation(summary = "Listar categorias de culto (opcional: filtro por igreja e ativo)")
    public List<CategoriaCultoResponse> listar(
            @RequestParam(required = false) UUID igrejaId,
            @RequestParam(required = false) Boolean ativo
    ) {
        return categoriaService.listar(igrejaId, ativo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar categoria de culto")
    public CategoriaCultoResponse buscar(@PathVariable UUID id) {
        return categoriaService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Criar categoria de culto (título, dia da semana, duração e pregador padrão)")
    public CategoriaCultoResponse criar(@Valid @RequestBody CategoriaCultoCreateRequest request) {
        return categoriaService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualizar categoria de culto")
    public CategoriaCultoResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody CategoriaCultoUpdateRequest request
    ) {
        return categoriaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Excluir categoria de culto (com cultos vinculados, apenas desativa)")
    public void excluir(@PathVariable UUID id) {
        categoriaService.excluir(id);
    }

    @PostMapping("/{id}/gerar-cultos")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Gerar os cultos desta categoria para o próximo mês (ou a competência informada)")
    public String gerarCultos(
            @PathVariable UUID id,
            @RequestParam(required = false) String competencia
    ) {
        if (competencia == null || competencia.isBlank()) {
            return automacaoService.gerarProximoMesDaCategoria(id);
        }
        return automacaoService.gerarMesDaCategoria(id, competencia);
    }
}