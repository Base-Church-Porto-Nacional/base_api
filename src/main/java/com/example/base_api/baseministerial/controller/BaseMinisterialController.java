package com.example.base_api.baseministerial.controller;

import com.example.base_api.baseministerial.dto.BaseMinisterialCreateRequest;
import com.example.base_api.baseministerial.dto.BaseMinisterialResponse;
import com.example.base_api.baseministerial.dto.BaseMinisterialUpdateRequest;
import com.example.base_api.baseministerial.service.BaseMinisterialService;
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
@RequestMapping("/api/bases-ministeriais")
@Tag(name = "Bases ministeriais")
@SecurityRequirement(name = "bearerAuth")
public class BaseMinisterialController {

    private final BaseMinisterialService baseMinisterialService;

    public BaseMinisterialController(BaseMinisterialService baseMinisterialService) {
        this.baseMinisterialService = baseMinisterialService;
    }

    @GetMapping
    @Operation(summary = "Listar bases ministeriais")
    public List<BaseMinisterialResponse> listar(@RequestParam(required = false) UUID igrejaId) {
        return baseMinisterialService.listar(igrejaId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar base ministerial por ID")
    public BaseMinisterialResponse buscar(@PathVariable UUID id) {
        return baseMinisterialService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Criar base ministerial")
    public BaseMinisterialResponse criar(@Valid @RequestBody BaseMinisterialCreateRequest request) {
        return baseMinisterialService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualizar base ministerial")
    public BaseMinisterialResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody BaseMinisterialUpdateRequest request
    ) {
        return baseMinisterialService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Excluir base ministerial e desvincular ministérios")
    public void excluir(@PathVariable UUID id) {
        baseMinisterialService.excluir(id);
    }
}