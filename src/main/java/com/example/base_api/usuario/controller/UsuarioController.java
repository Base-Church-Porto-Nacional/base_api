package com.example.base_api.usuario.controller;

import com.example.base_api.usuario.dto.LoginRequest;
import com.example.base_api.usuario.dto.LoginResponse;
import com.example.base_api.usuario.dto.UsuarioCreateRequest;
import com.example.base_api.usuario.dto.UsuarioResponse;
import com.example.base_api.usuario.dto.UsuarioUpdateRequest;
import com.example.base_api.usuario.service.UsuarioService;
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
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID")
    public UsuarioResponse buscar(@PathVariable UUID id) {
        return usuarioService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar usuário")
    public UsuarioResponse criar(@Valid @RequestBody UsuarioCreateRequest request) {
        return usuarioService.criar(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário")
    public UsuarioResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody UsuarioUpdateRequest request
    ) {
        return usuarioService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir usuário")
    public void excluir(@PathVariable UUID id) {
        usuarioService.excluir(id);
    }
}
