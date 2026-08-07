package com.cth.sdm.service;

import com.cth.sdm.model.Document;
import com.cth.sdm.model.DocumentStatus;
import com.cth.sdm.model.DocumentWorkflow;
import com.cth.sdm.repository.DocumentRepository;
import com.cth.sdm.repository.DocumentWorkflowRepository;
import com.cth.sdm.util.ClamAVScanner;
import com.cth.sdm.util.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentWorkflowRepository workflowRepository;
    private final StorageService storageService;
    private final ClamAVScanner clamAVScanner;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentWorkflowRepository workflowRepository,
            StorageService storageService,
            ClamAVScanner clamAVScanner) {
        this.documentRepository = documentRepository;
        this.workflowRepository = workflowRepository;
        this.storageService = storageService;
        this.clamAVScanner = clamAVScanner;
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public Optional<Document> findById(Long id) {
        return documentRepository.findById(id);
    }

    public Optional<Document> findByCode(String code) {
        return documentRepository.findByDocumentCode(code);
    }

    public List<Document> findBySdlcPhase(Integer phase) {
        return documentRepository.findBySdlcPhase(phase);
    }

    public Document uploadDocument(
            InputStream fileStream,
            String originalFilename,
            long size,
            String applicationCode,
            Integer sdlcPhase,
            String owner,
            String description) {

        String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toUpperCase();
        String savedPath = storageService.storeFile(fileStream, originalFilename);

        String appPrefix = applicationCode.substring(0, Math.min(3, applicationCode.length())).toUpperCase();
        long docCount = documentRepository.count() + 101;
        String documentCode = appPrefix + "-P" + docCount;

        Document doc = Document.builder()
                .documentCode(documentCode)
                .name(originalFilename)
                .fileType(ext)
                .fileSize(size)
                .filePath(savedPath)
                .status(DocumentStatus.UPLOADED)
                .applicationCode(appPrefix)
                .sdlcPhase(sdlcPhase)
                .version(1)
                .owner(owner)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();

        doc = documentRepository.save(doc);

        doc.setStatus(DocumentStatus.VALIDATING);
        documentRepository.save(doc);

        boolean isClean = clamAVScanner.isSafe(savedPath);
        if (!isClean) {
            doc.setStatus(DocumentStatus.FAILED);
            documentRepository.save(doc);
            throw new SecurityException("Malicious payload detected.");
        }

        doc.setStatus(DocumentStatus.PROCESSING);
        doc.setStatus(DocumentStatus.INDEXED);
        documentRepository.save(doc);

        DocumentWorkflow workflow = DocumentWorkflow.builder()
                .documentId(doc.getId())
                .step("MAKER")
                .action("UPLOADED")
                .actor(owner)
                .comments("File uploaded and verified.")
                .actionAt(LocalDateTime.now())
                .build();
        workflowRepository.save(workflow);

        return doc;
    }

    public Document submitForApproval(Long id, String actor) {
        Document doc = documentRepository.findById(id).orElseThrow();
        doc.setStatus(DocumentStatus.PENDING_APPROVAL);
        doc = documentRepository.save(doc);

        DocumentWorkflow workflow = DocumentWorkflow.builder()
                .documentId(doc.getId())
                .step("CHECKER")
                .action("SUBMITTED")
                .actor(actor)
                .comments("Submitted for checker approval.")
                .actionAt(LocalDateTime.now())
                .build();
        workflowRepository.save(workflow);

        return doc;
    }

    public Document approveDocument(Long id, String actor, String comment) {
        Document doc = documentRepository.findById(id).orElseThrow();
        doc.setStatus(DocumentStatus.APPROVED);
        doc = documentRepository.save(doc);

        DocumentWorkflow workflow = DocumentWorkflow.builder()
                .documentId(doc.getId())
                .step("APPROVER")
                .action("APPROVED")
                .actor(actor)
                .comments(comment)
                .actionAt(LocalDateTime.now())
                .build();
        workflowRepository.save(workflow);

        return doc;
    }

    public Document rejectDocument(Long id, String actor, String comment) {
        Document doc = documentRepository.findById(id).orElseThrow();
        doc.setStatus(DocumentStatus.REJECTED);
        doc = documentRepository.save(doc);

        DocumentWorkflow workflow = DocumentWorkflow.builder()
                .documentId(doc.getId())
                .step("APPROVER")
                .action("REJECTED")
                .actor(actor)
                .comments(comment)
                .actionAt(LocalDateTime.now())
                .build();
        workflowRepository.save(workflow);

        return doc;
    }
}
