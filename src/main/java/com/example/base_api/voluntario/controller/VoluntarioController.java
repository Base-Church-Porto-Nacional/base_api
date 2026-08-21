package com.example.base_api.voluntario.controller;

import com.example.base_api.voluntario.dto.PessoaServicoCreateRequest;
import com.example.base_api.voluntario.dto.PessoaServicoResponse;
import com.example.base_api.voluntario.dto.PessoaServicoUpdateRequest;
import com.example.base_api.voluntario.service.VoluntarioService;
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
@RequestMapping("/api/voluntarios")
@Tag(name = "Voluntários")
@SecurityRequirement(name = "bearerAuth")
public class VoluntarioController {

    private final VoluntarioService voluntarioService;

    public VoluntarioController(VoluntarioService voluntarioService) {
        this.voluntarioService = voluntarioService;
    }

    @GetMapping
    @Operation(summary = "Listar todos os vínculos de voluntariado")
    public List<PessoaServicoResponse> listar() {
        return voluntarioService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar vínculo de voluntariado por ID")
    public PessoaServicoResponse buscar(@PathVariable UUID id) {
        return voluntarioService.buscarPorId(id);
    }

    @GetMapping("/pessoa/{pessoaId}")
    @Operation(summary = "Listar vínculos de voluntariado de uma pessoa")
    public List<PessoaServicoResponse> listarPorPessoa(@PathVariable UUID pessoaId) {
        return voluntarioService.listarPorPessoa(pessoaId);
    }

    @GetMapping("/servico/{servicoId}")
    @Operation(summary = "Listar voluntários qualificados para um serviço")
    public List<PessoaServicoResponse> listarPorServico(@PathVariable UUID servicoId) {
        return voluntarioService.listarQualificadosPorServico(servicoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar vínculo de voluntariado")
    public PessoaServicoResponse vincular(@Valid @RequestBody PessoaServicoCreateRequest request) {
        return voluntarioService.vincular(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar vínculo (pessoa, serviço/ministério, cargo e demais campos)")
    public PessoaServicoResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PessoaServicoUpdateRequest request
    ) {
        return voluntarioService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir vínculo de voluntariado")
    public void desvincular(@PathVariable UUID id) {
        voluntarioService.desvincular(id);
    }
}
