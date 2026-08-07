package com.cth.sdm.application.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SdlcPhaseDto {
    private Long id;
    private String name;
    private String code;
    private String description;
    private List<DeliverableMappingDto> deliverables;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliverableMappingDto {
        private Long deliverableId;
        private String name;
        private String code;
        private String ownerUsername;
        private String latestVersion;
        private String approvalStatus;
    }
}
