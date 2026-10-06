package com.example.base_api.export.controller;

import com.example.base_api.export.dto.ExportFile;
import com.example.base_api.export.dto.FormatoExportacao;
import com.example.base_api.export.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export")
@Tag(name = "Exportação")
@SecurityRequirement(name = "bearerAuth")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/{recurso}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Exportar dados em texto, Markdown ou Excel",
            description = "Recursos: pessoas, jornadas, ministerios, servicos, voluntarios, escalas, disponibilidades, usuarios, cultos, completo. Formatos: txt, md, xlsx.")
    public ResponseEntity<byte[]> exportar(
            @PathVariable String recurso,
            @Parameter(description = "txt | md | xlsx")
            @RequestParam(defaultValue = "xlsx") String formato
    ) {
        FormatoExportacao formatoExportacao = FormatoExportacao.from(formato);
        ExportFile arquivo = exportService.exportar(recurso, formatoExportacao);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + arquivo.nomeArquivo() + "\"")
                .contentType(MediaType.parseMediaType(arquivo.contentType()))
                .body(arquivo.conteudo());
    }
}
