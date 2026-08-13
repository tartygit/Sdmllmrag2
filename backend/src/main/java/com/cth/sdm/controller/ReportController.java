package com.cth.sdm.controller;

import com.cth.sdm.model.Document;
import com.cth.sdm.service.DocumentService;
import com.cth.sdm.util.ReportGenerator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final DocumentService documentService;
    private final ReportGenerator reportGenerator;

    public ReportController(DocumentService documentService, ReportGenerator reportGenerator) {
        this.documentService = documentService;
        this.reportGenerator = reportGenerator;
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> getPdfReport() {
        List<Document> documents = documentService.getAllDocuments();
        byte[] pdfBytes = reportGenerator.generatePdfReport(documents);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "sdde-governance-report.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
