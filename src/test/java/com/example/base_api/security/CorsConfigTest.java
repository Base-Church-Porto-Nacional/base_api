package com.example.base_api.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Apps iOS/Android não enviam Origin e não podem ser barrados pelo CORS,
 * que só vale para o Flutter Web (navegador).
 */
class CorsConfigTest {

    private static final String FRONT_WEB = "https://base.veigagustavo.com.br";

    private final DefaultCorsProcessor processor = new DefaultCorsProcessor();

    private CorsConfiguration configuracao(String origens) {
        CorsProperties props = new CorsProperties();
        props.setAllowedOriginPatterns(origens);
        SecurityConfig config = new SecurityConfig(null, null, props, new MockEnvironment());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        return config.corsConfigurationSource().getCorsConfiguration(request);
    }

    private boolean processa(String origin) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setScheme("https");
        request.setServerName("api.veigagustavo.com.br");
        request.setServerPort(443);
        if (origin != null) {
            request.addHeader(HttpHeaders.ORIGIN, origin);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean aceito = processor.processRequest(configuracao(FRONT_WEB), request, response);
        return aceito && response.getStatus() == 200;
    }

    @Test
    void appNativoSemOriginPassa() throws Exception {
        assertThat(processa(null)).isTrue();
    }

    @Test
    void frontWebDeProducaoPassa() throws Exception {
        assertThat(processa(FRONT_WEB)).isTrue();
    }

    @Test
    void origemDesconhecidaEhBarrada() throws Exception {
        assertThat(processa("https://site-malicioso.com")).isFalse();
    }
}
