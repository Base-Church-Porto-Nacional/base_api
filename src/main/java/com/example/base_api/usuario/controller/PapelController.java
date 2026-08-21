package com.example.base_api.usuario.controller;

import com.example.base_api.usuario.dto.PapelRequest;
import com.example.base_api.usuario.dto.PapelResponse;
import com.example.base_api.usuario.service.PapelService;
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
@RequestMapping("/api/papeis")
@Tag(name = "Papéis")
@SecurityRequirement(name = "bearerAuth")
public class PapelController {

    private final PapelService papelService;

    public PapelController(PapelService papelService) {
        this.papelService = papelService;
    }

    @GetMapping
    @Operation(summary = "Listar papéis")
    public List<PapelResponse> listar() {
        return papelService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar papel por ID")
    public PapelResponse buscar(@PathVariable UUID id) {
        return papelService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar papel")
    public PapelResponse criar(@Valid @RequestBody PapelRequest request) {
        return papelService.criar(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar papel")
    public PapelResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PapelRequest request
    ) {
        return papelService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir papel")
    public void excluir(@PathVariable UUID id) {
        papelService.excluir(id);
    }
}
