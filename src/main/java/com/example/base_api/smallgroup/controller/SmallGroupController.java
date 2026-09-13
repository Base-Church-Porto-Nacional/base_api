package com.example.base_api.smallgroup.controller;

import com.example.base_api.smallgroup.dto.SmallGroupCreateRequest;
import com.example.base_api.smallgroup.dto.SmallGroupEncontroRequest;
import com.example.base_api.smallgroup.dto.SmallGroupEncontroResponse;
import com.example.base_api.smallgroup.dto.SmallGroupResponse;
import com.example.base_api.smallgroup.dto.SmallGroupUpdateRequest;
import com.example.base_api.smallgroup.service.SmallGroupService;
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
@RequestMapping("/api/small-groups")
@Tag(name = "Pequenos grupos")
@SecurityRequirement(name = "bearerAuth")
public class SmallGroupController {

    private final SmallGroupService smallGroupService;

    public SmallGroupController(SmallGroupService smallGroupService) {
        this.smallGroupService = smallGroupService;
    }

    @GetMapping
    @Operation(summary = "Listar pequenos grupos")
    public List<SmallGroupResponse> listar(@RequestParam(required = false) UUID igrejaId) {
        return smallGroupService.listar(igrejaId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pequeno grupo por ID")
    public SmallGroupResponse buscar(@PathVariable UUID id) {
        return smallGroupService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Criar pequeno grupo")
    public SmallGroupResponse criar(@Valid @RequestBody SmallGroupCreateRequest request) {
        return smallGroupService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualizar pequeno grupo")
    public SmallGroupResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody SmallGroupUpdateRequest request
    ) {
        return smallGroupService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Excluir pequeno grupo")
    public void excluir(@PathVariable UUID id) {
        smallGroupService.excluir(id);
    }

    @PostMapping("/{smallGroupId}/encontros")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Registrar encontro de um pequeno grupo")
    public SmallGroupEncontroResponse criarEncontro(
            @PathVariable UUID smallGroupId,
            @Valid @RequestBody SmallGroupEncontroRequest request
    ) {
        return smallGroupService.criarEncontro(smallGroupId, request);
    }

    @PutMapping("/encontros/{encontroId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualizar encontro de um pequeno grupo")
    public SmallGroupEncontroResponse atualizarEncontro(
            @PathVariable UUID encontroId,
            @Valid @RequestBody SmallGroupEncontroRequest request
    ) {
        return smallGroupService.atualizarEncontro(encontroId, request);
    }
}