package com.cth.sdm.infrastructure.security;

import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class FileExtensionValidator {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "PDF", "DOCX", "XLSX", "PPTX", "XML", "HTML", "TXT", "CSV", "ZIP"
    );

    public boolean isValid(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return false;
        }

        String extension = fileName.substring(lastDotIndex + 1).toUpperCase().trim();
        return ALLOWED_EXTENSIONS.contains(extension);
    }
}
