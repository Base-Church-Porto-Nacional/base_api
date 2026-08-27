package com.example.base_api.igreja.controller;

import com.example.base_api.igreja.dto.IgrejaFilialCreateRequest;
import com.example.base_api.igreja.dto.IgrejaResponse;
import com.example.base_api.igreja.dto.IgrejaUpdateRequest;
import com.example.base_api.igreja.service.IgrejaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/igrejas")
@Tag(name = "Igrejas")
@SecurityRequirement(name = "bearerAuth")
public class IgrejaController {

    private final IgrejaService igrejaService;

    public IgrejaController(IgrejaService igrejaService) {
        this.igrejaService = igrejaService;
    }

    @GetMapping
    @Operation(summary = "Listar igrejas visíveis (matriz vê a rede; filial só a própria)")
    public List<IgrejaResponse> listar() {
        return igrejaService.listarVisiveis();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar igreja por ID")
    public IgrejaResponse buscar(@PathVariable UUID id) {
        return igrejaService.buscarPorId(id);
    }

    @PostMapping("/{matrizId}/filiais")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Criar filial da matriz, com pastor e admin")
    public IgrejaResponse criarFilial(
            @PathVariable UUID matrizId,
            @Valid @RequestBody IgrejaFilialCreateRequest request
    ) {
        return igrejaService.criarFilial(matrizId, request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualizar igreja")
    public IgrejaResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody IgrejaUpdateRequest request
    ) {
        return igrejaService.atualizar(id, request);
    }
}
