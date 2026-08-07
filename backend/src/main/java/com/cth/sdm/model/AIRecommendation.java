package com.cth.sdm.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ai_recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long documentId;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String missingSections;

    private Integer complianceScore;

    @Column(columnDefinition = "TEXT")
    private String suggestedImprovements;

    @Column(columnDefinition = "TEXT")
    private String similarDocuments;

    @Column(columnDefinition = "TEXT")
    private String riskAssessment;

    private Integer qualityScore;

    @Column(columnDefinition = "TEXT")
    private String duplicateDetection;
}
