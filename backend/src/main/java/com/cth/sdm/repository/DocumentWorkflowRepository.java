package com.cth.sdm.repository;

import com.cth.sdm.model.DocumentWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentWorkflowRepository extends JpaRepository<DocumentWorkflow, Long> {
    List<DocumentWorkflow> findByDocumentId(Long documentId);
}
