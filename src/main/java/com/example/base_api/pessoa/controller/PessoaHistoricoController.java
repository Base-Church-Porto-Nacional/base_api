package com.example.base_api.pessoa.controller;

import com.example.base_api.pessoa.dto.PessoaHistoricoCreateRequest;
import com.example.base_api.pessoa.dto.PessoaHistoricoResponse;
import com.example.base_api.pessoa.dto.PessoaHistoricoUpdateRequest;
import com.example.base_api.pessoa.service.PessoaHistoricoService;
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
@RequestMapping("/api/pessoas/{pessoaId}/historico")
@Tag(name = "Histórico da Pessoa")
@SecurityRequirement(name = "bearerAuth")
public class PessoaHistoricoController {

    private final PessoaHistoricoService pessoaHistoricoService;

    public PessoaHistoricoController(PessoaHistoricoService pessoaHistoricoService) {
        this.pessoaHistoricoService = pessoaHistoricoService;
    }

    @GetMapping
    @Operation(summary = "Listar histórico da pessoa")
    public List<PessoaHistoricoResponse> listar(@PathVariable UUID pessoaId) {
        return pessoaHistoricoService.listarPorPessoa(pessoaId);
    }

    @GetMapping("/{historicoId}")
    @Operation(summary = "Buscar registro do histórico")
    public PessoaHistoricoResponse buscar(
            @PathVariable UUID pessoaId,
            @PathVariable UUID historicoId
    ) {
        return pessoaHistoricoService.buscarPorId(pessoaId, historicoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adicionar registro ao histórico")
    public PessoaHistoricoResponse criar(
            @PathVariable UUID pessoaId,
            @Valid @RequestBody PessoaHistoricoCreateRequest request
    ) {
        return pessoaHistoricoService.criar(pessoaId, request);
    }

    @PutMapping("/{historicoId}")
    @Operation(summary = "Atualizar registro do histórico")
    public PessoaHistoricoResponse atualizar(
            @PathVariable UUID pessoaId,
            @PathVariable UUID historicoId,
            @Valid @RequestBody PessoaHistoricoUpdateRequest request
    ) {
        return pessoaHistoricoService.atualizar(pessoaId, historicoId, request);
    }

    @DeleteMapping("/{historicoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir registro do histórico")
    public void excluir(
            @PathVariable UUID pessoaId,
            @PathVariable UUID historicoId
    ) {
        pessoaHistoricoService.excluir(pessoaId, historicoId);
    }
}
