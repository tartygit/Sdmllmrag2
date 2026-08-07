package com.cth.sdm.application.service;

import com.cth.sdm.domain.model.Document;
import com.cth.sdm.domain.model.DocumentIndex;
import com.cth.sdm.domain.model.DocumentVersion;
import com.cth.sdm.domain.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("databaseFallbackSearchService")
@Slf4j
@RequiredArgsConstructor
public class DatabaseFallbackSearchService implements SearchService {

    private final DocumentRepository documentRepository;

    @Override
    public void indexDocument(Document document, String fullTextContent) {
        log.info("[FALLBACK INDEX] Simulating document indexing to database search tables for document ID: {}", document.getId());
    }

    @Override
    public List<DocumentIndex> search(String query, String phaseCode, String appCode, String owner, String status) {
        log.info("[FALLBACK SEARCH] Executing multi-field database fallback search: query={}, phase={}, appCode={}, owner={}, status={}",
                query, phaseCode, appCode, owner, status);

        List<Document> allDocs = documentRepository.findAll();

        return allDocs.stream()
                .filter(doc -> {
                    // Filter by SDLC Phase Code
                    if (phaseCode != null && !phaseCode.isBlank()) {
                        if (doc.getDeliverable() == null || doc.getDeliverable().getPhase() == null ||
                                !phaseCode.equalsIgnoreCase(doc.getDeliverable().getPhase().getCode())) {
                            return false;
                        }
                    }
                    return true;
                })
                .filter(doc -> {
                    // Filter by Owner Username
                    if (owner != null && !owner.isBlank()) {
                        if (doc.getOwner() == null || !owner.equalsIgnoreCase(doc.getOwner().getUsername())) {
                            return false;
                        }
                    }
                    return true;
                })
                .filter(doc -> {
                    // Filter by Status
                    if (status != null && !status.isBlank()) {
                        if (doc.getStatus() == null || !status.equalsIgnoreCase(doc.getStatus().name())) {
                            return false;
                        }
                    }
                    return true;
                })
                .filter(doc -> {
                    // Filter by Application Code (from auto-generated document number or title)
                    if (appCode != null && !appCode.isBlank()) {
                        if (doc.getDocumentNumber() == null || !doc.getDocumentNumber().toUpperCase().startsWith(appCode.toUpperCase())) {
                            return false;
                        }
                    }
                    return true;
                })
                .filter(doc -> {
                    // Filter by query (full-text search keyword matching Title, Description, or content)
                    if (query != null && !query.isBlank()) {
                        String q = query.toLowerCase();
                        boolean titleMatch = doc.getTitle() != null && doc.getTitle().toLowerCase().contains(q);
                        boolean descMatch = doc.getDescription() != null && doc.getDescription().toLowerCase().contains(q);

                        return titleMatch || descMatch;
                    }
                    return true;
                })
                .map(this::mapToDocumentIndex)
                .collect(Collectors.toList());
    }

    private DocumentIndex mapToDocumentIndex(Document doc) {
        String verNum = "1.0.0";
        if (doc.getVersions() != null && !doc.getVersions().isEmpty()) {
            DocumentVersion latest = doc.getVersions().get(doc.getVersions().size() - 1);
            verNum = latest.getVersionNumber();
        }

        return DocumentIndex.builder()
                .id(doc.getId().toString())
                .documentId(doc.getId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .status(doc.getStatus() != null ? doc.getStatus().name() : null)
                .owner(doc.getOwner() != null ? doc.getOwner().getUsername() : null)
                .appCode(doc.getDocumentNumber() != null && doc.getDocumentNumber().contains("-") ? doc.getDocumentNumber().split("-")[0] : "SDDE")
                .phaseCode(doc.getDeliverable() != null && doc.getDeliverable().getPhase() != null ? doc.getDeliverable().getPhase().getCode() : null)
                .versionNumber(verNum)
                .build();
    }
}
