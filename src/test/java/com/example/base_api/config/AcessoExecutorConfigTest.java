package com.example.base_api.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(OutputCaptureExtension.class)
class AcessoExecutorConfigTest {

    @Test
    void rejeicaoDescartaComAvisoNoLog(CapturedOutput saida) {
        ThreadPoolTaskExecutor executor = new AcessoExecutorConfig().acessoExecutor();
        executor.initialize();
        try {
            RejectedExecutionHandler handler = executor.getThreadPoolExecutor().getRejectedExecutionHandler();
            assertThat(handler).isNotInstanceOf(ThreadPoolExecutor.DiscardPolicy.class);

            assertThatCode(() -> handler.rejectedExecution(() -> { }, executor.getThreadPoolExecutor()))
                    .doesNotThrowAnyException();
            assertThat(saida.getOut()).contains("WARN").contains("descartado");
        } finally {
            executor.shutdown();
        }
    }
}
