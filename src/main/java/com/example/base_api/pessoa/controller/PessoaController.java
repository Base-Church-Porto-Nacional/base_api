package com.example.base_api.pessoa.controller;

import com.example.base_api.pessoa.dto.PessoaCreateRequest;
import com.example.base_api.pessoa.dto.PessoaResponse;
import com.example.base_api.pessoa.dto.PessoaUpdateRequest;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.usuario.dto.PessoaUsuarioCreateRequest;
import com.example.base_api.usuario.dto.UsuarioResponse;
import com.example.base_api.usuario.service.UsuarioService;
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
@RequestMapping("/api/pessoas")
@Tag(name = "Pessoas")
@SecurityRequirement(name = "bearerAuth")
public class PessoaController {

    private final PessoaService pessoaService;
    private final UsuarioService usuarioService;

    public PessoaController(PessoaService pessoaService, UsuarioService usuarioService) {
        this.pessoaService = pessoaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Listar pessoas")
    public List<PessoaResponse> listar(@RequestParam(required = false) UUID igrejaId) {
        return pessoaService.listar(igrejaId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pessoa por ID")
    public PessoaResponse buscar(@PathVariable UUID id) {
        return pessoaService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cadastrar pessoa")
    public PessoaResponse criar(@Valid @RequestBody PessoaCreateRequest request) {
        return pessoaService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualizar pessoa")
    public PessoaResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PessoaUpdateRequest request
    ) {
        return pessoaService.atualizar(id, request);
    }

    @PostMapping("/{id}/usuario")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Criar usuário a partir da pessoa, com papéis da igreja")
    public UsuarioResponse criarUsuario(
            @PathVariable UUID id,
            @Valid @RequestBody PessoaUsuarioCreateRequest request
    ) {
        return usuarioService.criarAPartirDaPessoa(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Excluir pessoa")
    public void excluir(@PathVariable UUID id) {
        pessoaService.excluir(id);
    }
}
