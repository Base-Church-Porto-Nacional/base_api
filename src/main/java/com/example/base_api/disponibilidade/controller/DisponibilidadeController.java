package com.example.base_api.disponibilidade.controller;

import com.example.base_api.disponibilidade.dto.DisponibilidadeSemanalRequest;
import com.example.base_api.disponibilidade.dto.DisponibilidadeSemanalResponse;
import com.example.base_api.disponibilidade.dto.IndisponibilidadeRequest;
import com.example.base_api.disponibilidade.dto.IndisponibilidadeResponse;
import com.example.base_api.disponibilidade.service.DisponibilidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/disponibilidades")
@Tag(name = "Disponibilidade")
@SecurityRequirement(name = "bearerAuth")
public class DisponibilidadeController {

    private final DisponibilidadeService disponibilidadeService;

    public DisponibilidadeController(DisponibilidadeService disponibilidadeService) {
        this.disponibilidadeService = disponibilidadeService;
    }

    @GetMapping("/semanal/pessoa/{pessoaId}")
    @Operation(summary = "Listar disponibilidade semanal da pessoa")
    public List<DisponibilidadeSemanalResponse> listarSemanal(@PathVariable UUID pessoaId) {
        return disponibilidadeService.listarSemanal(pessoaId);
    }

    @PostMapping("/semanal")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar disponibilidade semanal")
    public DisponibilidadeSemanalResponse criarSemanal(
            @Valid @RequestBody DisponibilidadeSemanalRequest request
    ) {
        return disponibilidadeService.criarSemanal(request);
    }

    @DeleteMapping("/semanal/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir disponibilidade semanal")
    public void excluirSemanal(@PathVariable UUID id) {
        disponibilidadeService.excluirSemanal(id);
    }

    @GetMapping("/indisponibilidades/pessoa/{pessoaId}")
    @Operation(summary = "Listar indisponibilidades da pessoa")
    public List<IndisponibilidadeResponse> listarIndisponibilidades(@PathVariable UUID pessoaId) {
        return disponibilidadeService.listarIndisponibilidades(pessoaId);
    }

    @PostMapping("/indisponibilidades")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar indisponibilidade")
    public IndisponibilidadeResponse criarIndisponibilidade(
            @Valid @RequestBody IndisponibilidadeRequest request
    ) {
        return disponibilidadeService.criarIndisponibilidade(request);
    }

    @DeleteMapping("/indisponibilidades/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir indisponibilidade")
    public void excluirIndisponibilidade(@PathVariable UUID id) {
        disponibilidadeService.excluirIndisponibilidade(id);
    }
}
