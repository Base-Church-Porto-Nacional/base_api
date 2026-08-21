package com.example.base_api.jornada.controller;

import com.example.base_api.jornada.dto.JornadaResponse;
import com.example.base_api.jornada.dto.JornadaUpdateRequest;
import com.example.base_api.jornada.dto.PessoaEtapaRequest;
import com.example.base_api.jornada.dto.PessoaEtapaResponse;
import com.example.base_api.jornada.service.JornadaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/pessoas/{pessoaId}/jornada")
@Tag(name = "Jornada")
@SecurityRequirement(name = "bearerAuth")
public class JornadaController {

    private final JornadaService jornadaService;

    public JornadaController(JornadaService jornadaService) {
        this.jornadaService = jornadaService;
    }

    @GetMapping
    @Operation(summary = "Consultar jornada principal da pessoa")
    public JornadaResponse buscar(@PathVariable UUID pessoaId) {
        return jornadaService.buscarPorPessoa(pessoaId);
    }

    @PutMapping
    @Operation(summary = "Atualizar jornada principal da pessoa")
    public JornadaResponse atualizar(
            @PathVariable UUID pessoaId,
            @Valid @RequestBody JornadaUpdateRequest request
    ) {
        return jornadaService.atualizar(pessoaId, request);
    }

    @GetMapping("/etapas")
    @Operation(summary = "Listar etapas extensíveis da jornada")
    public List<PessoaEtapaResponse> listarEtapas(@PathVariable UUID pessoaId) {
        return jornadaService.listarEtapasDaPessoa(pessoaId);
    }

    @PostMapping("/etapas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar ou atualizar etapa extensível da jornada")
    public PessoaEtapaResponse registrarEtapa(
            @PathVariable UUID pessoaId,
            @Valid @RequestBody PessoaEtapaRequest request
    ) {
        return jornadaService.registrarEtapa(pessoaId, request);
    }
}
