package com.example.base_api.export;

import java.util.ArrayList;
import java.util.List;

public class ExportSheet {

    private final String titulo;
    private final List<String> cabecalhos;
    private final List<List<String>> linhas = new ArrayList<>();

    public ExportSheet(String titulo, List<String> cabecalhos) {
        this.titulo = titulo;
        this.cabecalhos = List.copyOf(cabecalhos);
    }

    public void adicionarLinha(List<String> valores) {
        linhas.add(valores.stream().map(v -> v == null ? "" : v).toList());
    }

    public String getTitulo() {
        return titulo;
    }

    public List<String> getCabecalhos() {
        return cabecalhos;
    }

    public List<List<String>> getLinhas() {
        return linhas;
    }
}
