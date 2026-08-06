package com.cth.sdm.application.service;

import com.cth.sdm.application.dto.DocumentIngestedEvent;
import com.cth.sdm.domain.model.*;
import com.cth.sdm.domain.repository.DocumentRepository;
import com.cth.sdm.infrastructure.security.ClamAvScanner;
import com.cth.sdm.infrastructure.security.FileExtensionValidator;
import com.cth.sdm.infrastructure.security.SecurityAuditService;
import com.cth.sdm.infrastructure.storage.StorageService;
import com.cth.sdm.infrastructure.messaging.RabbitEventDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentIngestionService implements IngestionHandler {

    private final StorageService storageService;
    private final ClamAvScanner clamAvScanner;
    private final FileExtensionValidator fileExtensionValidator;
    private final DocumentRepository documentRepository;
    private final RabbitEventDispatcher rabbitEventDispatcher;
    private final SecurityAuditService securityAuditService;

    @Override
    @Transactional
    public void ingest(InputStream stream, String originalFileName, String contentType, User owner) {
        log.info("Starting ingestion pipeline for file: {}", originalFileName);

        // 1. File extension check
        if (!fileExtensionValidator.isValid(originalFileName)) {
            String errorMsg = "Unsupported file format/extension for file: " + originalFileName;
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        byte[] fileBytes;
        try {
            fileBytes = stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read ingestion stream", e);
        }

        // 2. Scan every stream using ClamAV prior to persistence
        try (InputStream scanStream = new ByteArrayInputStream(fileBytes)) {
            boolean isClean = clamAvScanner.scanStream(scanStream);
            if (!isClean) {
                securityAuditService.logLoginFailure(owner != null ? owner.getUsername() : "system",
                        "VIRUS_DETECTED: Infected file " + originalFileName + " was blocked and quarantined.", "0.0.0.0");
                throw new SecurityException("Infected stream blocked by anti-virus scan!");
            }
        } catch (IOException e) {
            log.error("Antivirus scan IOException: {}", e.getMessage());
            throw new RuntimeException("Failed during antivirus scan execution", e);
        }

        // 3. Store file into Storage Engine
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;
        String storagePath = "documents/" + uniqueFileName;

        try (InputStream storeStream = new ByteArrayInputStream(fileBytes)) {
            storageService.store(storagePath, storeStream, fileBytes.length, contentType);
        } catch (IOException e) {
            throw new RuntimeException("Storage persist failed", e);
        }

        // 4. Save Core Domain Entities and Mappings to DB
        Document document = Document.builder()
                .title(originalFileName)
                .description("Automatically ingested file deliverable")
                .status(DocumentStatus.DRAFT)
                .owner(owner)
                .build();

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber("1.0.0")
                .filePath(storagePath)
                .fileSize((long) fileBytes.length)
                .contentType(contentType)
                .createdBy(owner)
                .build();

        DocumentMetadata metadata = DocumentMetadata.builder()
                .documentVersion(version)
                .author(owner != null ? owner.getFullName() : "Ingestion Engine")
                .securityClassification("INTERNAL")
                .build();

        version.setMetadata(metadata);
        document.setVersions(java.util.List.of(version));

        Document savedDoc = documentRepository.save(document);
        log.info("Saved Document to database with ID: {}", savedDoc.getId());

        // 5. Publish Event to RabbitMQ
        DocumentIngestedEvent event = DocumentIngestedEvent.builder()
                .documentId(savedDoc.getId())
                .title(savedDoc.getTitle())
                .versionNumber("1.0.0")
                .filePath(storagePath)
                .owner(owner != null ? owner.getUsername() : "system")
                .ingestedAt(LocalDateTime.now())
                .build();

        rabbitEventDispatcher.dispatchDocumentIngestedEvent(event);

        // 6. Security Audit Log
        securityAuditService.logLoginSuccess(owner != null ? owner.getUsername() : "system", "0.0.0.0");
    }
}
