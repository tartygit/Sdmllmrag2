package com.cth.sdm.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long documentId;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, length = 255)
    private String filePath;

    @Column(nullable = false)
    private String author;

    private String comments;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
