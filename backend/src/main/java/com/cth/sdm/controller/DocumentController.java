package com.cth.sdm.controller;

import com.cth.sdm.model.Document;
import com.cth.sdm.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.findById(id).orElseThrow());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<Document> getDocumentByCode(@PathVariable String code) {
        return ResponseEntity.ok(documentService.findByCode(code).orElseThrow());
    }

    @GetMapping("/phase/{phase}")
    public ResponseEntity<List<Document>> getDocumentsByPhase(@PathVariable Integer phase) {
        return ResponseEntity.ok(documentService.findBySdlcPhase(phase));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('MAKER', 'ADMIN')")
    public ResponseEntity<Document> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("applicationCode") String applicationCode,
            @RequestParam("sdlcPhase") Integer sdlcPhase,
            @RequestParam(value = "description", required = false) String description,
            Principal principal) {
        try {
            Document doc = documentService.uploadDocument(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getSize(),
                    applicationCode,
                    sdlcPhase,
                    principal.getName(),
                    description
            );
            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('MAKER', 'ADMIN')")
    public ResponseEntity<Document> submitForApproval(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(documentService.submitForApproval(id, principal.getName()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('APPROVER', 'CHECKER', 'ADMIN')")
    public ResponseEntity<Document> approveDocument(
            @PathVariable Long id,
            @RequestParam("comments") String comments,
            Principal principal) {
        return ResponseEntity.ok(documentService.approveDocument(id, principal.getName(), comments));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('APPROVER', 'CHECKER', 'ADMIN')")
    public ResponseEntity<Document> rejectDocument(
            @PathVariable Long id,
            @RequestParam("comments") String comments,
            Principal principal) {
        return ResponseEntity.ok(documentService.rejectDocument(id, principal.getName(), comments));
    }
}
