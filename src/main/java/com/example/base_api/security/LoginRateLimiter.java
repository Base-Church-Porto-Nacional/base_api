package com.example.base_api.security;

import com.example.base_api.common.exception.TooManyRequestsException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    static final String ESCOPO_LOGIN = "login";
    static final int MAX_TENTATIVAS = 5;
    static final Duration JANELA = Duration.ofMinutes(15);

    private final Map<String, Deque<Long>> tentativas = new ConcurrentHashMap<>();

    public void assertPermitido(String identificador, String ip) {
        assertPermitido(ESCOPO_LOGIN, identificador, ip);
    }

    public void registrarFalha(String identificador, String ip) {
        registrarFalha(ESCOPO_LOGIN, identificador, ip);
    }

    public void limpar(String identificador, String ip) {
        limpar(ESCOPO_LOGIN, identificador, ip);
    }

    /** Contador independente por escopo (login, token, esqueci); a chave só é normalizada no escopo de login. */
    public void assertPermitido(String escopo, String chave, String ip) {
        String chaveCompleta = chave(escopo, chave, ip);
        long agora = System.currentTimeMillis();
        long inicioJanela = agora - JANELA.toMillis();
        Deque<Long> fila = tentativas.computeIfAbsent(chaveCompleta, k -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && fila.peekFirst() < inicioJanela) {
                fila.removeFirst();
            }
            if (fila.size() >= MAX_TENTATIVAS) {
                throw new TooManyRequestsException("Muitas tentativas. Aguarde alguns minutos e tente de novo.");
            }
        }
    }

    public void registrarFalha(String escopo, String chaveBruta, String ip) {
        String chave = chave(escopo, chaveBruta, ip);
        long agora = System.currentTimeMillis();
        Deque<Long> fila = tentativas.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (fila) {
            fila.addLast(agora);
        }
    }

    public void limpar(String escopo, String chave, String ip) {
        tentativas.remove(chave(escopo, chave, ip));
    }

    private String chave(String escopo, String identificador, String ip) {
        // Mesma chave para "(63) 99999-0000" e "63999990000": o limite não se burla trocando o formato.
        String user = ESCOPO_LOGIN.equals(escopo) ? Identificador.normalizado(identificador) : identificador;
        String origem = ip == null || ip.isBlank() ? "unknown" : ip;
        return escopo + "|" + user + "|" + origem;
    }

    /** Remove periodicamente as chaves sem tentativas recentes, para o mapa não crescer indefinidamente. */
    @Scheduled(cron = "0 */15 * * * *", zone = "${app.timezone:America/Sao_Paulo}")
    public void limparExpirados() {
        limparExpiradosParaTeste(System.currentTimeMillis());
    }

    void limparExpiradosParaTeste(long agora) {
        long inicioJanela = agora - JANELA.toMillis();
        for (Iterator<Map.Entry<String, Deque<Long>>> it = tentativas.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Deque<Long>> entry = it.next();
            Deque<Long> fila = entry.getValue();
            synchronized (fila) {
                while (!fila.isEmpty() && fila.peekFirst() < inicioJanela) {
                    fila.removeFirst();
                }
                if (fila.isEmpty()) {
                    it.remove();
                }
            }
        }
    }
}
