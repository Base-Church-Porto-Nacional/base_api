package com.example.base_api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;

@Configuration
public class AcessoExecutorConfig {

    private static final Logger log = LoggerFactory.getLogger(AcessoExecutorConfig.class);

    /** Executor do envio de links de acesso (esqueci a senha), fora da thread da requisição. */
    @Bean(name = "acessoExecutor")
    public ThreadPoolTaskExecutor acessoExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("acesso-");
        executor.setRejectedExecutionHandler(descartarComAviso());
        return executor;
    }

    /** Fila cheia: descarta o envio (a resposta já foi 204) e deixa rastro no log, sem dados pessoais. */
    static RejectedExecutionHandler descartarComAviso() {
        return (tarefa, pool) -> log.warn(
                "Envio de link de acesso descartado: fila do executor cheia (ativos={}, fila={})",
                pool.getActiveCount(), pool.getQueue().size());
    }
}
