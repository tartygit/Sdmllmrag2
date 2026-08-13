package com.cth.sdm.repository;

import com.cth.sdm.model.AIRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AIRecommendationRepository extends JpaRepository<AIRecommendation, Long> {
    Optional<AIRecommendation> findByDocumentId(Long documentId);
}
