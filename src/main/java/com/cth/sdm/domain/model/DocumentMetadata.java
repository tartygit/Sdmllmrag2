package com.cth.sdm.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_metadata")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;

    @Column(length = 150)
    private String author;

    @Column(name = "security_classification", length = 100)
    private String securityClassification;

    @Column(length = 255)
    private String keywords;

    @Column(length = 100)
    private String department;

    @Column(length = 50)
    private String language;
}
