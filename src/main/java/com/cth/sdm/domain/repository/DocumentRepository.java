package com.cth.sdm.domain.repository;

import com.cth.sdm.domain.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}
