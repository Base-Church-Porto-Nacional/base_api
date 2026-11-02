package com.example.base_api.usuario.controller;

import com.example.base_api.security.LoginRateLimiter;
import com.example.base_api.usuario.dto.DefinirSenhaRequest;
import com.example.base_api.usuario.dto.EsqueciSenhaRequest;
import com.example.base_api.usuario.dto.LoginRequest;
import com.example.base_api.usuario.dto.LoginResponse;
import com.example.base_api.usuario.dto.TokenInfoResponse;
import com.example.base_api.usuario.service.AcessoService;
import com.example.base_api.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final UsuarioService usuarioService;
    private final LoginRateLimiter loginRateLimiter;
    private final AcessoService acessoService;

    public AuthController(UsuarioService usuarioService, LoginRateLimiter loginRateLimiter, AcessoService acessoService) {
        this.usuarioService = usuarioService;
        this.acessoService = acessoService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar e obter token JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = ipCliente(httpRequest);
        loginRateLimiter.assertPermitido(request.identificador(), ip);
        try {
            LoginResponse response = usuarioService.autenticar(request);
            loginRateLimiter.limpar(request.identificador(), ip);
            return response;
        } catch (RuntimeException ex) {
            loginRateLimiter.registrarFalha(request.identificador(), ip);
            throw ex;
        }
    }

    @GetMapping("/token/{token}")
    @Operation(summary = "Validar link de convite/redefinição e obter o primeiro nome")
    public TokenInfoResponse info(@PathVariable String token, HttpServletRequest httpRequest) {
        return acessoService.info(token, ipCliente(httpRequest));
    }

    @PostMapping("/definir-senha")
    @Operation(summary = "Definir senha com link de uso único e já autenticar")
    public LoginResponse definirSenha(@Valid @RequestBody DefinirSenhaRequest request, HttpServletRequest httpRequest) {
        return acessoService.definirSenha(request, ipCliente(httpRequest));
    }

    @PostMapping("/esqueci-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Solicitar link de nova senha por WhatsApp (sempre 204)")
    public void esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest request, HttpServletRequest httpRequest) {
        acessoService.solicitarRedefinicao(request.identificador(), ipCliente(httpRequest));
    }

    /**
     * IP do cliente. Não lê X-Forwarded-For diretamente (o cliente poderia forjá-lo e burlar o rate limit):
     * com server.forward-headers-strategy=NATIVE o Tomcat só aplica o header quando a conexão vem de um
     * proxy interno confiável, e já entrega o IP real em getRemoteAddr().
     */
    private String ipCliente(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
