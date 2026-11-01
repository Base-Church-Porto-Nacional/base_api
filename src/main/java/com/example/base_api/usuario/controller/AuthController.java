package com.example.base_api.usuario.controller;

import com.example.base_api.security.LoginRateLimiter;
import com.example.base_api.usuario.dto.LoginRequest;
import com.example.base_api.usuario.dto.LoginResponse;
import com.example.base_api.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final UsuarioService usuarioService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(UsuarioService usuarioService, LoginRateLimiter loginRateLimiter) {
        this.usuarioService = usuarioService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar e obter token JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = ipCliente(httpRequest);
        loginRateLimiter.assertPermitido(request.username(), ip);
        try {
            LoginResponse response = usuarioService.autenticar(request);
            loginRateLimiter.limpar(request.username(), ip);
            return response;
        } catch (RuntimeException ex) {
            loginRateLimiter.registrarFalha(request.username(), ip);
            throw ex;
        }
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
