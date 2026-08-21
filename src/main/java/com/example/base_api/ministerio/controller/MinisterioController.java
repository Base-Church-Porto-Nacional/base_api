package com.example.base_api.ministerio.controller;

import com.example.base_api.ministerio.dto.MinisterioCreateRequest;
import com.example.base_api.ministerio.dto.MinisterioResponse;
import com.example.base_api.ministerio.dto.MinisterioUpdateRequest;
import com.example.base_api.ministerio.service.MinisterioService;
import com.example.base_api.servico.dto.ServicoCreateRequest;
import com.example.base_api.servico.dto.ServicoResponse;
import com.example.base_api.servico.service.ServicoService;
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
@RequestMapping("/api/ministerios")
@Tag(name = "Ministérios")
@SecurityRequirement(name = "bearerAuth")
public class MinisterioController {

    private final MinisterioService ministerioService;
    private final ServicoService servicoService;

    public MinisterioController(MinisterioService ministerioService, ServicoService servicoService) {
        this.ministerioService = ministerioService;
        this.servicoService = servicoService;
    }

    @GetMapping
    @Operation(summary = "Listar ministérios")
    public List<MinisterioResponse> listar() {
        return ministerioService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar ministério por ID")
    public MinisterioResponse buscar(@PathVariable UUID id) {
        return ministerioService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar ministério")
    public MinisterioResponse criar(@Valid @RequestBody MinisterioCreateRequest request) {
        return ministerioService.criar(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar ministério")
    public MinisterioResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody MinisterioUpdateRequest request
    ) {
        return ministerioService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir ministério")
    public void excluir(@PathVariable UUID id) {
        ministerioService.excluir(id);
    }

    @GetMapping("/{id}/servicos")
    @Operation(summary = "Listar serviços de um ministério")
    public List<ServicoResponse> listarServicos(@PathVariable UUID id) {
        return servicoService.listarPorMinisterio(id);
    }

    @PostMapping("/{id}/servicos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar serviço em um ministério")
    public ServicoResponse criarServico(
            @PathVariable UUID id,
            @Valid @RequestBody ServicoCreateRequest request
    ) {
        return servicoService.criar(id, request);
    }
}
