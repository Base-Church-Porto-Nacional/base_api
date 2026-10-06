package com.example.base_api.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/cultos/1");

    @Test
    void corpoInvalidoDeveSer400() {
        ResponseEntity<ErrorResponse> resposta = handler.handleNotReadable(
                new HttpMessageNotReadableException("vazio", new MockHttpInputMessage(new byte[0])), request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().message()).contains("JSON inválido");
    }

    @Test
    void parametroComTipoInvalidoDeveSer400() {
        ResponseEntity<ErrorResponse> resposta = handler.handleTypeMismatch(
                new MethodArgumentTypeMismatchException("abc", UUID.class, "igrejaId", null, null), request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().message()).contains("igrejaId");
    }

    @Test
    void parametroObrigatorioAusenteDeveSer400() {
        ResponseEntity<ErrorResponse> resposta = handler.handleMissingParameter(
                new MissingServletRequestParameterException("competencia", "String"), request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().message()).contains("competencia");
    }

    @Test
    void rotaInexistenteDeveSer404() {
        ResponseEntity<ErrorResponse> resposta = handler.handleNoResource(
                new NoResourceFoundException(HttpMethod.GET, "/api/inexistente", "api/inexistente"), request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void metodoNaoSuportadoDeveSer405() {
        ResponseEntity<ErrorResponse> resposta = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("PATCH"), request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void erroInesperadoDeveSer500SemVazarDetalhes() {
        ResponseEntity<ErrorResponse> resposta = handler.handleGeneric(new IllegalStateException("segredo"), request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().message()).doesNotContain("segredo");
    }
}
