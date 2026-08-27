package com.example.base_api.export.service;

import com.example.base_api.export.dto.ExportDocument;
import com.example.base_api.export.dto.ExportFile;
import com.example.base_api.export.dto.ExportSheet;
import com.example.base_api.export.dto.FormatoExportacao;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExportRendererTest {

    private final ExportRenderer renderer = new ExportRenderer();

    @Test
    void deveGerarMarkdownSimples() {
        ExportDocument doc = new ExportDocument("pessoas");
        ExportSheet sheet = new ExportSheet("Pessoas", List.of("Nome", "Email"));
        sheet.adicionarLinha(List.of("João", "joao@email.com"));
        doc.adicionarSheet(sheet);

        ExportFile file = renderer.renderizar(doc, FormatoExportacao.MD);

        String conteudo = new String(file.conteudo(), StandardCharsets.UTF_8);
        assertThat(file.nomeArquivo()).isEqualTo("pessoas.md");
        assertThat(conteudo).contains("# Pessoas");
        assertThat(conteudo).contains("| Nome | Email |");
        assertThat(conteudo).contains("| João | joao@email.com |");
    }

    @Test
    void deveGerarTextoSimples() {
        ExportDocument doc = new ExportDocument("ministerios");
        ExportSheet sheet = new ExportSheet("Ministerios", List.of("Nome"));
        sheet.adicionarLinha(List.of("Louvor"));
        doc.adicionarSheet(sheet);

        ExportFile file = renderer.renderizar(doc, FormatoExportacao.TXT);

        String conteudo = new String(file.conteudo(), StandardCharsets.UTF_8);
        assertThat(file.nomeArquivo()).isEqualTo("ministerios.txt");
        assertThat(conteudo).contains("Ministerios");
        assertThat(conteudo).contains("Louvor");
    }

    @Test
    void deveGerarExcel() {
        ExportDocument doc = new ExportDocument("completo");
        ExportSheet sheet = new ExportSheet("Pessoas", List.of("Nome"));
        sheet.adicionarLinha(List.of("Maria"));
        doc.adicionarSheet(sheet);

        ExportFile file = renderer.renderizar(doc, FormatoExportacao.XLSX);

        assertThat(file.nomeArquivo()).isEqualTo("completo.xlsx");
        assertThat(file.contentType()).contains("spreadsheet");
        assertThat(file.conteudo().length).isGreaterThan(0);
    }
}
