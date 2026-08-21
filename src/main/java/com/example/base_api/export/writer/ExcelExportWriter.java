package com.example.base_api.export.writer;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.export.ExportDocument;
import com.example.base_api.export.ExportSheet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public final class ExcelExportWriter {

    private ExcelExportWriter() {
    }

    public static byte[] escrever(ExportDocument document) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (ExportSheet exportSheet : document.getSheets()) {
                Sheet sheet = workbook.createSheet(sanitizarNomeAba(exportSheet.getTitulo()));

                Row headerRow = sheet.createRow(0);
                List<String> cabecalhos = exportSheet.getCabecalhos();
                for (int c = 0; c < cabecalhos.size(); c++) {
                    Cell cell = headerRow.createCell(c);
                    cell.setCellValue(cabecalhos.get(c));
                    cell.setCellStyle(headerStyle);
                }

                List<List<String>> linhas = exportSheet.getLinhas();
                for (int r = 0; r < linhas.size(); r++) {
                    Row row = sheet.createRow(r + 1);
                    List<String> valores = linhas.get(r);
                    for (int c = 0; c < valores.size(); c++) {
                        row.createCell(c).setCellValue(valores.get(c));
                    }
                }

                for (int c = 0; c < cabecalhos.size(); c++) {
                    sheet.autoSizeColumn(c);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Falha ao gerar arquivo Excel");
        }
    }

    private static String sanitizarNomeAba(String titulo) {
        String nome = titulo.replaceAll("[\\\\/*?\\[\\]:]", " ").trim();
        if (nome.length() > 31) {
            nome = nome.substring(0, 31);
        }
        return nome.isBlank() ? "Dados" : nome;
    }
}
