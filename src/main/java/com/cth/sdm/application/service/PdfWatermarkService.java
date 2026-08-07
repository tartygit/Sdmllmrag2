package com.cth.sdm.application.service;

import com.lowagie.text.Element;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
@Slf4j
public class PdfWatermarkService {

    public byte[] addWatermark(InputStream pdfStream, String watermarkText) {
        if (pdfStream == null || watermarkText == null || watermarkText.isBlank()) {
            throw new IllegalArgumentException("PDF stream and watermark text must not be null/empty.");
        }

        log.info("Applying digital watermark '{}' to PDF stream...", watermarkText);

        try {
            PdfReader reader = new PdfReader(pdfStream);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfStamper stamper = new PdfStamper(reader, out);

            int totalPages = reader.getNumberOfPages();
            BaseFont font = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);

            // Configure transparent graphics state for watermark
            PdfGState gstate = new PdfGState();
            gstate.setFillOpacity(0.35f); // 35% opacity
            gstate.setStrokeOpacity(0.35f);

            for (int i = 1; i <= totalPages; i++) {
                // Get page direct content (layered on top)
                PdfContentByte over = stamper.getOverContent(i);
                over.saveState();
                over.setGState(gstate);
                over.beginText();
                over.setFontAndSize(font, 55);
                over.setColorFill(new java.awt.Color(180, 180, 180)); // Light Gray

                // Center position (standard A4 is ~595x842)
                float x = reader.getPageSize(i).getWidth() / 2;
                float y = reader.getPageSize(i).getHeight() / 2;

                // Stamp text diagonally in the center of the page at 45 degrees
                over.showTextAligned(Element.ALIGN_CENTER, watermarkText.toUpperCase(), x, y, 45);
                over.endText();
                over.restoreState();
            }

            stamper.close();
            reader.close();

            log.info("Successfully stamped digital watermark onto {} pages.", totalPages);
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Failed to apply PDF watermark: {}", e.getMessage());
            throw new RuntimeException("Watermarking failed", e);
        }
    }
}
