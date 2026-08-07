package com.cth.sdm.application.service;

import com.cth.sdm.domain.model.DocumentVersion;
import com.cth.sdm.infrastructure.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfPreviewEngine implements PreviewEngine {

    private final StorageService storageService;

    @Override
    public InputStream generatePreview(DocumentVersion version) {
        if (version == null || version.getFilePath() == null) {
            log.error("Cannot generate preview for null version or path.");
            return new ByteArrayInputStream("EMPTY DOCUMENT PREVIEW".getBytes(StandardCharsets.UTF_8));
        }

        log.info("Generating read-only preview for file path: {}", version.getFilePath());

        try {
            // Load file stream from configured Storage Engine
            InputStream originalStream = storageService.load(version.getFilePath());
            return originalStream;
        } catch (Exception e) {
            log.error("Failed to load document stream for preview: {}", e.getMessage());
            return new ByteArrayInputStream(("FAILED_PREVIEW: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }
}
