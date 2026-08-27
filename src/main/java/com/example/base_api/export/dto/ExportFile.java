package com.example.base_api.export.dto;

public record ExportFile(
        String nomeArquivo,
        String contentType,
        byte[] conteudo
) {
}
