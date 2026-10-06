package com.example.base_api.aniversario.controller;

import com.example.base_api.aniversario.dto.MensagemAniversarioRequest;
import com.example.base_api.aniversario.dto.MensagemAniversarioResponse;
import com.example.base_api.aniversario.service.MensagemAniversarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/mensagens/aniversario")
@Tag(name = "Aniversários")
@SecurityRequirement(name = "bearerAuth")
public class MensagemAniversarioController {

    private final MensagemAniversarioService service;

    public MensagemAniversarioController(MensagemAniversarioService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obter a mensagem padrão de aniversário da igreja (própria ou herdada)")
    public MensagemAniversarioResponse obter(@RequestParam(required = false) UUID igrejaId) {
        return service.obter(igrejaId);
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Salvar a mensagem padrão de aniversário da igreja")
    public MensagemAniversarioResponse salvar(@Valid @RequestBody MensagemAniversarioRequest request) {
        return service.salvar(request);
    }
}
