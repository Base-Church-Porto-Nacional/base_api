package com.example.base_api.export.service;

import com.example.base_api.export.dto.ExportDocument;
import com.example.base_api.export.dto.ExportFile;
import com.example.base_api.export.dto.FormatoExportacao;
import com.example.base_api.export.writer.ExcelExportWriter;
import com.example.base_api.export.writer.TextExportWriter;
import org.springframework.stereotype.Component;

@Component
public class ExportRenderer {

    public ExportFile renderizar(ExportDocument document, FormatoExportacao formato) {
        byte[] conteudo = switch (formato) {
            case TXT, MD -> TextExportWriter.escrever(document, formato);
            case XLSX -> ExcelExportWriter.escrever(document);
        };

        String nomeArquivo = document.getNomeArquivoBase() + "." + formato.getExtensao();
        return new ExportFile(nomeArquivo, formato.getContentType(), conteudo);
    }
}
