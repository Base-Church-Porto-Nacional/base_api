package com.example.base_api.usuario.controller;

import com.example.base_api.usuario.dto.LinkAcessoResponse;
import com.example.base_api.usuario.dto.UsuarioPapeisRequest;
import com.example.base_api.usuario.dto.UsuarioResponse;
import com.example.base_api.usuario.service.AcessoService;
import com.example.base_api.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AcessoService acessoService;

    public UsuarioController(UsuarioService usuarioService, AcessoService acessoService) {
        this.usuarioService = usuarioService;
        this.acessoService = acessoService;
    }

    @PostMapping("/{id}/redefinicao-senha")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Gerar link de redefinição de senha (vale 1 hora) e tentar enviar por WhatsApp")
    public LinkAcessoResponse gerarRedefinicao(@PathVariable UUID id) {
        return acessoService.gerarRedefinicao(id);
    }

    @GetMapping
    @Operation(summary = "Listar usuários")
    public List<UsuarioResponse> listar(@RequestParam(required = false) UUID igrejaId) {
        return usuarioService.listar(igrejaId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID")
    public UsuarioResponse buscar(@PathVariable UUID id) {
        return usuarioService.buscarPorId(id);
    }

    @PutMapping("/{id}/papeis")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Define o conjunto de papéis do usuário (PASTOR/SECRETARIA só pelo pastor; papéis customizados também pela secretaria)")
    public UsuarioResponse alterarPapeis(
            @PathVariable UUID id,
            @Valid @RequestBody UsuarioPapeisRequest request
    ) {
        return usuarioService.alterarPapeis(id, request);
    }

    @PostMapping("/{id}/desativar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Desativar conta: invalida sessões e links pendentes")
    public UsuarioResponse desativar(@PathVariable UUID id) {
        return usuarioService.desativar(id);
    }

    @PostMapping("/{id}/reativar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Reativar conta desativada (volta convidada se nunca definiu senha)")
    public UsuarioResponse reativar(@PathVariable UUID id) {
        return usuarioService.reativar(id);
    }
}
