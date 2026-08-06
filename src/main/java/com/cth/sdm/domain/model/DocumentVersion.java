package com.cth.sdm.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_versions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "version_number", nullable = false, length = 50)
    private String versionNumber;

    @Column(name = "file_path", nullable = false, length = 512)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "change_summary", length = 500)
    private String changeSummary;

    @OneToOne(mappedBy = "documentVersion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DocumentMetadata metadata;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
