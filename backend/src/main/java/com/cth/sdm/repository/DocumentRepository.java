package com.cth.sdm.repository;

import com.cth.sdm.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByDocumentCode(String documentCode);
    List<Document> findBySdlcPhase(Integer sdlcPhase);
}
