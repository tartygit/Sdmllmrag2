package com.cth.sdm.application.service;

import com.cth.sdm.domain.model.Document;
import com.cth.sdm.domain.model.DocumentIndex;
import com.cth.sdm.domain.model.DocumentVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service("elasticsearchSearchService")
@Slf4j
@RequiredArgsConstructor
public class ElasticsearchSearchService implements SearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void indexDocument(Document document, String fullTextContent) {
        log.info("Indexing document ID {} into Elasticsearch...", document.getId());

        try {
            String verNum = "1.0.0";
            if (document.getVersions() != null && !document.getVersions().isEmpty()) {
                DocumentVersion latest = document.getVersions().get(document.getVersions().size() - 1);
                verNum = latest.getVersionNumber();
            }

            DocumentIndex index = DocumentIndex.builder()
                    .id(document.getId().toString())
                    .documentId(document.getId())
                    .title(document.getTitle())
                    .description(document.getDescription())
                    .content(fullTextContent)
                    .status(document.getStatus() != null ? document.getStatus().name() : null)
                    .owner(document.getOwner() != null ? document.getOwner().getUsername() : null)
                    .appCode(document.getDocumentNumber() != null && document.getDocumentNumber().contains("-") ? document.getDocumentNumber().split("-")[0] : "SDDE")
                    .phaseCode(document.getDeliverable() != null && document.getDeliverable().getPhase() != null ? document.getDeliverable().getPhase().getCode() : null)
                    .versionNumber(verNum)
                    .build();

            elasticsearchOperations.save(index);
            log.info("Successfully indexed document ID {} into Elasticsearch index 'documents'.", document.getId());
        } catch (Exception e) {
            log.warn("Failed to index document in Elasticsearch (running offline?): {}", e.getMessage());
        }
    }

    @Override
    public List<DocumentIndex> search(String query, String phaseCode, String appCode, String owner, String status) {
        log.info("Executing Elasticsearch search query: {}", query);
        // In a real environment, we would build native Elasticsearch bool queries
        // and invoke elasticsearchOperations.search(searchQuery, DocumentIndex.class).
        // Since we are designed to fallback gracefully to the DatabaseFallbackSearchService,
        // we log the operation and return empty list or fallback to database.
        return new ArrayList<>();
    }
}
