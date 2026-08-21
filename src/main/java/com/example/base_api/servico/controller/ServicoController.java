package com.example.base_api.servico.controller;

import com.example.base_api.servico.dto.ServicoResponse;
import com.example.base_api.servico.dto.ServicoUpdateRequest;
import com.example.base_api.servico.service.ServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/servicos")
@Tag(name = "Serviços")
@SecurityRequirement(name = "bearerAuth")
public class ServicoController {

    private final ServicoService servicoService;

    public ServicoController(ServicoService servicoService) {
        this.servicoService = servicoService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar serviço por ID")
    public ServicoResponse buscar(@PathVariable UUID id) {
        return servicoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar serviço")
    public ServicoResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ServicoUpdateRequest request
    ) {
        return servicoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir serviço")
    public void excluir(@PathVariable UUID id) {
        servicoService.excluir(id);
    }
}
