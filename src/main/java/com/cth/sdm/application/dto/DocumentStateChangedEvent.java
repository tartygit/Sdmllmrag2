package com.cth.sdm.application.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStateChangedEvent {
    private Long documentId;
    private String documentTitle;
    private String newStatus;
    private String recipientEmail;
    private String recipientPhone;
}
