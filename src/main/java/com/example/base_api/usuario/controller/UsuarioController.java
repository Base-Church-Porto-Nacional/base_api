package com.example.base_api.usuario.controller;

import com.example.base_api.usuario.dto.UsuarioCreateRequest;
import com.example.base_api.usuario.dto.UsuarioGestaoRequest;
import com.example.base_api.usuario.dto.UsuarioResponse;
import com.example.base_api.usuario.dto.UsuarioUpdateRequest;
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
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Listar usuários")
    public List<UsuarioResponse> listar(@RequestParam(required = false) UUID igrejaId) {
        return usuarioService.listar(igrejaId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID")
    public UsuarioResponse buscar(@PathVariable UUID id) {
        return usuarioService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Criar usuário")
    public UsuarioResponse criar(@Valid @RequestBody UsuarioCreateRequest request) {
        return usuarioService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualizar usuário")
    public UsuarioResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody UsuarioUpdateRequest request
    ) {
        return usuarioService.atualizar(id, request);
    }

    @PostMapping("/{id}/gestao")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Promover ADMIN/PASTOR na igreja alvo (somente pastor/admin da igreja ou da matriz)")
    public UsuarioResponse promoverGestao(
            @PathVariable UUID id,
            @Valid @RequestBody UsuarioGestaoRequest request
    ) {
        return usuarioService.promoverGestao(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Excluir usuário")
    public void excluir(@PathVariable UUID id) {
        usuarioService.excluir(id);
    }
}
