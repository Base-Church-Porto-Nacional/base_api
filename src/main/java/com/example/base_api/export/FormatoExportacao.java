package com.example.base_api.export;

import com.example.base_api.common.exception.BusinessException;

public enum FormatoExportacao {
    TXT("txt", "text/plain"),
    MD("md", "text/markdown"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final String extensao;
    private final String contentType;

    FormatoExportacao(String extensao, String contentType) {
        this.extensao = extensao;
        this.contentType = contentType;
    }

    public String getExtensao() {
        return extensao;
    }

    public String getContentType() {
        return contentType;
    }

    public static FormatoExportacao from(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessException("Informe o formato: txt, md ou xlsx");
        }
        return switch (valor.trim().toLowerCase()) {
            case "txt", "texto", "text" -> TXT;
            case "md", "markdown" -> MD;
            case "xlsx", "excel", "xls" -> XLSX;
            default -> throw new BusinessException("Formato inválido. Use txt, md ou xlsx");
        };
    }
}
