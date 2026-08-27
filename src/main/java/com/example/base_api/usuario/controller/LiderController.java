package com.example.base_api.usuario.controller;

import com.example.base_api.usuario.dto.LiderResponse;
import com.example.base_api.usuario.service.LiderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lideres")
@Tag(name = "Líderes")
@SecurityRequirement(name = "bearerAuth")
public class LiderController {

    private final LiderService liderService;

    public LiderController(LiderService liderService) {
        this.liderService = liderService;
    }

    @GetMapping
    @Operation(summary = "Listar líderes (usuários ativos com papel LIDER)")
    public List<LiderResponse> listar(@RequestParam(required = false) UUID igrejaId) {
        return liderService.listar(igrejaId);
    }
}
