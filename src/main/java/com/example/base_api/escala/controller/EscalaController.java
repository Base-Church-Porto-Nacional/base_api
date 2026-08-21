package com.example.base_api.escala.controller;

import com.example.base_api.escala.dto.EscalaCreateRequest;
import com.example.base_api.escala.dto.EscalaResponse;
import com.example.base_api.escala.dto.EscalaUpdateRequest;
import com.example.base_api.escala.dto.EscalaVoluntarioCreateRequest;
import com.example.base_api.escala.dto.EscalaVoluntarioStatusRequest;
import com.example.base_api.escala.service.EscalaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/escalas")
@Tag(name = "Escalas")
@SecurityRequirement(name = "bearerAuth")
public class EscalaController {

    private final EscalaService escalaService;

    public EscalaController(EscalaService escalaService) {
        this.escalaService = escalaService;
    }

    @GetMapping
    @Operation(summary = "Listar escalas")
    public List<EscalaResponse> listar() {
        return escalaService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar escala por ID")
    public EscalaResponse buscar(@PathVariable UUID id) {
        return escalaService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar escala")
    public EscalaResponse criar(@Valid @RequestBody EscalaCreateRequest request) {
        return escalaService.criar(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar escala")
    public EscalaResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody EscalaUpdateRequest request
    ) {
        return escalaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir escala")
    public void excluir(@PathVariable UUID id) {
        escalaService.excluir(id);
    }

    @PostMapping("/{id}/voluntarios")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Designar voluntário para a escala")
    public EscalaResponse.EscalaVoluntarioResponse designarVoluntario(
            @PathVariable UUID id,
            @Valid @RequestBody EscalaVoluntarioCreateRequest request
    ) {
        return escalaService.designarVoluntario(id, request);
    }

    @PutMapping("/{id}/voluntarios/{designacaoId}/status")
    @Operation(summary = "Confirmar, recusar ou substituir designação")
    public EscalaResponse.EscalaVoluntarioResponse atualizarStatus(
            @PathVariable UUID id,
            @PathVariable UUID designacaoId,
            @Valid @RequestBody EscalaVoluntarioStatusRequest request
    ) {
        return escalaService.atualizarStatus(id, designacaoId, request);
    }
}
