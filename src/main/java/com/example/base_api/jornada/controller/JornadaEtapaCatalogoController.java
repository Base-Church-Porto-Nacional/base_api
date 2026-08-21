package com.example.base_api.jornada.controller;

import com.example.base_api.jornada.dto.JornadaEtapaCatalogoRequest;
import com.example.base_api.jornada.dto.JornadaEtapaCatalogoResponse;
import com.example.base_api.jornada.service.JornadaEtapaCatalogoService;
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
@RequestMapping("/api/jornada-etapas")
@Tag(name = "Catálogo de Etapas da Jornada")
@SecurityRequirement(name = "bearerAuth")
public class JornadaEtapaCatalogoController {

    private final JornadaEtapaCatalogoService service;

    public JornadaEtapaCatalogoController(JornadaEtapaCatalogoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar etapas do catálogo")
    public List<JornadaEtapaCatalogoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar etapa do catálogo")
    public JornadaEtapaCatalogoResponse buscar(@PathVariable UUID id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar etapa no catálogo")
    public JornadaEtapaCatalogoResponse criar(@Valid @RequestBody JornadaEtapaCatalogoRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar etapa do catálogo")
    public JornadaEtapaCatalogoResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody JornadaEtapaCatalogoRequest request
    ) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir etapa do catálogo")
    public void excluir(@PathVariable UUID id) {
        service.excluir(id);
    }
}
