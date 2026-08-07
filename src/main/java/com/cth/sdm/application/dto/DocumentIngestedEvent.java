package com.cth.sdm.application.dto;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentIngestedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long documentId;
    private String title;
    private String versionNumber;
    private String filePath;
    private String owner;
    private LocalDateTime ingestedAt;
}
