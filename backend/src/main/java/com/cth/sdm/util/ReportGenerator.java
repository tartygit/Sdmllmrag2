package com.cth.sdm.util;

import com.cth.sdm.model.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Component
public class ReportGenerator {

    public byte[] generatePdfReport(List<Document> documents) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        com.lowagie.text.Document document = new com.lowagie.text.Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            document.add(new Paragraph("SDDE Compliance & Governance Document Report", titleFont));
            document.add(new Paragraph("Generated on: " + java.time.LocalDateTime.now().toString()));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.addCell("Code");
            table.addCell("Name");
            table.addCell("Status");
            table.addCell("Owner");
            table.addCell("Version");

            for (Document doc : documents) {
                table.addCell(doc.getDocumentCode());
                table.addCell(doc.getName());
                table.addCell(doc.getStatus().name());
                table.addCell(doc.getOwner());
                table.addCell(String.valueOf(doc.getVersion()));
            }

            document.add(table);
            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF report", e);
        }
        return out.toByteArray();
    }
}
