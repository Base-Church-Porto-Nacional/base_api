package com.example.base_api.culto.controller;

import com.example.base_api.culto.service.CultoAutomacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/cultos/automacao")
@Tag(name = "Gestão de Cultos — Automação")
@SecurityRequirement(name = "bearerAuth")
public class CultoAutomacaoController {

    private final CultoAutomacaoService automacaoService;

    public CultoAutomacaoController(CultoAutomacaoService automacaoService) {
        this.automacaoService = automacaoService;
    }

    @PostMapping("/proximo-mes")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Gerar os cultos do próximo mês para as igrejas visíveis (idempotente)")
    public String gerarProximoMes(@RequestParam(required = false) UUID igrejaId) {
        return automacaoService.gerarProximoMesManual(igrejaId);
    }

    @PostMapping("/mes/{competencia}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Gerar os cultos de uma competência específica (ex.: 2026-09)")
    public String gerarMes(
            @PathVariable String competencia,
            @RequestParam(required = false) UUID igrejaId
    ) {
        return automacaoService.gerarMesManual(competencia, igrejaId);
    }
}