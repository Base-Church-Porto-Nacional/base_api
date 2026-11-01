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

    static final int MAX_TENTATIVAS = 5;
    static final Duration JANELA = Duration.ofMinutes(15);

    private final Map<String, Deque<Long>> tentativas = new ConcurrentHashMap<>();

    public void assertPermitido(String username, String ip) {
        String chave = chave(username, ip);
        long agora = System.currentTimeMillis();
        long inicioJanela = agora - JANELA.toMillis();
        Deque<Long> fila = tentativas.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && fila.peekFirst() < inicioJanela) {
                fila.removeFirst();
            }
            if (fila.size() >= MAX_TENTATIVAS) {
                throw new TooManyRequestsException("Muitas tentativas de login. Tente novamente em alguns minutos");
            }
        }
    }

    public void registrarFalha(String username, String ip) {
        String chave = chave(username, ip);
        long agora = System.currentTimeMillis();
        Deque<Long> fila = tentativas.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (fila) {
            fila.addLast(agora);
        }
    }

    public void limpar(String username, String ip) {
        tentativas.remove(chave(username, ip));
    }

    private String chave(String username, String ip) {
        String user = username == null ? "" : username.trim().toLowerCase();
        String origem = ip == null || ip.isBlank() ? "unknown" : ip;
        return user + "|" + origem;
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
