package com.cth.sdm.application.service;

import com.cth.sdm.domain.model.Document;
import com.cth.sdm.domain.model.DocumentIndex;
import java.util.List;

public interface SearchService {
    void indexDocument(Document document, String fullTextContent);
    List<DocumentIndex> search(String query, String phaseCode, String appCode, String owner, String status);
}
