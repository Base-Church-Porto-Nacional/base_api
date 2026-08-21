package com.example.base_api.export.writer;

import com.example.base_api.export.ExportDocument;
import com.example.base_api.export.ExportSheet;
import com.example.base_api.export.FormatoExportacao;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public final class TextExportWriter {

    private TextExportWriter() {
    }

    public static byte[] escrever(ExportDocument document, FormatoExportacao formato) {
        StringBuilder sb = new StringBuilder();
        boolean markdown = formato == FormatoExportacao.MD;

        for (int i = 0; i < document.getSheets().size(); i++) {
            ExportSheet sheet = document.getSheets().get(i);
            if (i > 0) {
                sb.append(System.lineSeparator()).append(System.lineSeparator());
            }

            if (markdown) {
                sb.append("# ").append(sheet.getTitulo()).append(System.lineSeparator())
                        .append(System.lineSeparator());
                escreverMarkdownTabela(sb, sheet);
            } else {
                sb.append(sheet.getTitulo()).append(System.lineSeparator());
                sb.append("=".repeat(Math.max(sheet.getTitulo().length(), 3))).append(System.lineSeparator());
                escreverTextoSimples(sb, sheet);
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void escreverMarkdownTabela(StringBuilder sb, ExportSheet sheet) {
        sb.append("| ").append(String.join(" | ", sheet.getCabecalhos())).append(" |")
                .append(System.lineSeparator());
        sb.append("| ").append(sheet.getCabecalhos().stream()
                        .map(h -> "---")
                        .collect(Collectors.joining(" | ")))
                .append(" |")
                .append(System.lineSeparator());

        for (List<String> linha : sheet.getLinhas()) {
            sb.append("| ").append(linha.stream()
                            .map(TextExportWriter::escaparMarkdown)
                            .collect(Collectors.joining(" | ")))
                    .append(" |")
                    .append(System.lineSeparator());
        }
    }

    private static void escreverTextoSimples(StringBuilder sb, ExportSheet sheet) {
        sb.append(String.join(" | ", sheet.getCabecalhos())).append(System.lineSeparator());
        sb.append("-".repeat(60)).append(System.lineSeparator());
        for (List<String> linha : sheet.getLinhas()) {
            sb.append(String.join(" | ", linha)).append(System.lineSeparator());
        }
        if (sheet.getLinhas().isEmpty()) {
            sb.append("(sem registros)").append(System.lineSeparator());
        }
    }

    private static String escaparMarkdown(String valor) {
        if (valor == null || valor.isBlank()) {
            return "";
        }
        return valor.replace("|", "\\|").replace("\n", " ").replace("\r", "");
    }
}
