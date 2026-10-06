package com.example.base_api.aniversario.dto;

import com.example.base_api.aniversario.entity.AniversarioEnvio;
import com.example.base_api.aniversario.entity.StatusEnvioAniversario;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AniversarioEnvioResponse(
        UUID id,
        UUID pessoaId,
        UUID igrejaId,
        LocalDate dataAniversario,
        Instant dataProcessamento,
        String mensagem,
        StatusEnvioAniversario status,
        String erro
) {

    public static AniversarioEnvioResponse from(AniversarioEnvio envio) {
        return new AniversarioEnvioResponse(
                envio.getId(),
                envio.getPessoaId(),
                envio.getIgrejaId(),
                envio.getDataAniversario(),
                envio.getDataProcessamento(),
                envio.getMensagem(),
                envio.getStatus(),
                envio.getErro()
        );
    }
}