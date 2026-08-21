package com.example.base_api.export;

public record ExportFile(
        String nomeArquivo,
        String contentType,
        byte[] conteudo
) {
}
