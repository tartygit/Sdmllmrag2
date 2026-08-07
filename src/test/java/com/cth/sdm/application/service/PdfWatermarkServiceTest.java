package com.cth.sdm.application.service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class PdfWatermarkServiceTest {

    private final PdfWatermarkService pdfWatermarkService = new PdfWatermarkService();

    @Test
    void shouldApplyWatermarkToPdf() throws Exception {
        // 1. Create a simple mock PDF in memory using OpenPDF
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph("SDDE Core Deliverable Specification Content"));
        document.close();

        byte[] originalPdfBytes = out.toByteArray();
        assertNotNull(originalPdfBytes);
        assertTrue(originalPdfBytes.length > 0);

        // 2. Pass stream to PdfWatermarkService
        InputStream originalStream = new ByteArrayInputStream(originalPdfBytes);
        byte[] watermarkedBytes = pdfWatermarkService.addWatermark(originalStream, "APPROVED");

        assertNotNull(watermarkedBytes);
        assertTrue(watermarkedBytes.length > originalPdfBytes.length); // Should contain additional watermark instructions

        // Check PDF header
        String header = new String(watermarkedBytes, 0, 4);
        assertEquals("%PDF", header);
    }
}
