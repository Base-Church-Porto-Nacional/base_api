package com.example.base_api.usuario.controller;

import com.example.base_api.usuario.dto.MeResponse;
import com.example.base_api.usuario.dto.MeUpdateRequest;
import com.example.base_api.usuario.service.MeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Meu Perfil")
@SecurityRequirement(name = "bearerAuth")
public class MeController {

    private final MeService meService;

    public MeController(MeService meService) {
        this.meService = meService;
    }

    @GetMapping
    @Operation(summary = "Obter dados do usuário autenticado e pessoa vinculada")
    public MeResponse obter(Authentication authentication) {
        return meService.obterPerfil(authentication);
    }

    @PutMapping
    @Operation(summary = "Atualizar meus dados (usuário + pessoa vinculada)")
    public MeResponse atualizar(
            Authentication authentication,
            @Valid @RequestBody MeUpdateRequest request
    ) {
        return meService.atualizarPerfil(authentication, request);
    }
}
