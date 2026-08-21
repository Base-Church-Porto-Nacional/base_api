package com.example.base_api.usuario.controller;

import com.example.base_api.usuario.dto.LoginRequest;
import com.example.base_api.usuario.dto.LoginResponse;
import com.example.base_api.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar e obter token JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return usuarioService.autenticar(request);
    }
}
