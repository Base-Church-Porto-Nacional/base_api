package com.example.base_api.pessoa.controller;

import com.example.base_api.pessoa.dto.PessoaPausaCreateRequest;
import com.example.base_api.pessoa.dto.PessoaPausaResponse;
import com.example.base_api.pessoa.dto.PessoaPausaUpdateRequest;
import com.example.base_api.pessoa.service.PessoaPausaService;
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
@RequestMapping("/api/pessoas/{pessoaId}/pausas")
@Tag(name = "Pausas")
@SecurityRequirement(name = "bearerAuth")
public class PessoaPausaController {

    private final PessoaPausaService pessoaPausaService;

    public PessoaPausaController(PessoaPausaService pessoaPausaService) {
        this.pessoaPausaService = pessoaPausaService;
    }

    @GetMapping
    @Operation(summary = "Listar pausas da pessoa")
    public List<PessoaPausaResponse> listar(@PathVariable UUID pessoaId) {
        return pessoaPausaService.listarPorPessoa(pessoaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar pausa no perfil")
    public PessoaPausaResponse criar(
            @PathVariable UUID pessoaId,
            @Valid @RequestBody PessoaPausaCreateRequest request
    ) {
        return pessoaPausaService.criar(pessoaId, request);
    }

    @PutMapping("/{pausaId}")
    @Operation(summary = "Atualizar pausa")
    public PessoaPausaResponse atualizar(
            @PathVariable UUID pessoaId,
            @PathVariable UUID pausaId,
            @Valid @RequestBody PessoaPausaUpdateRequest request
    ) {
        return pessoaPausaService.atualizar(pessoaId, pausaId, request);
    }

    @DeleteMapping("/{pausaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir pausa")
    public void excluir(
            @PathVariable UUID pessoaId,
            @PathVariable UUID pausaId
    ) {
        pessoaPausaService.excluir(pessoaId, pausaId);
    }
}
