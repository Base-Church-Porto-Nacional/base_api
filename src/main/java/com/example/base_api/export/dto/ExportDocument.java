package com.example.base_api.export.dto;

import java.util.ArrayList;
import java.util.List;

public class ExportDocument {

    private final String nomeArquivoBase;
    private final List<ExportSheet> sheets = new ArrayList<>();

    public ExportDocument(String nomeArquivoBase) {
        this.nomeArquivoBase = nomeArquivoBase;
    }

    public void adicionarSheet(ExportSheet sheet) {
        sheets.add(sheet);
    }

    public String getNomeArquivoBase() {
        return nomeArquivoBase;
    }

    public List<ExportSheet> getSheets() {
        return sheets;
    }
}
