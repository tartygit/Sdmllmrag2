package com.cth.sdm.application.service;

import com.cth.sdm.application.dto.SdlcPhaseDto;
import com.cth.sdm.domain.model.Document;
import com.cth.sdm.domain.model.DocumentVersion;
import com.cth.sdm.domain.model.SDLCDeliverable;
import com.cth.sdm.domain.model.SDLCPhase;
import com.cth.sdm.domain.repository.DocumentRepository;
import com.cth.sdm.domain.repository.SDLCDeliverableRepository;
import com.cth.sdm.domain.repository.SDLCPhaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdlcPhaseService {

    private final SDLCPhaseRepository sdlcPhaseRepository;
    private final SDLCDeliverableRepository sdlcDeliverableRepository;
    private final DocumentRepository documentRepository;

    @Transactional(readOnly = true)
    public List<SdlcPhaseDto> getAllPhases() {
        log.info("Retrieving all SDLC Phases and compiling deliverable mappings...");
        List<SDLCPhase> phases = sdlcPhaseRepository.findAll();
        List<Document> allDocuments = documentRepository.findAll();

        return phases.stream()
                .map(phase -> compilePhaseDto(phase, allDocuments))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SdlcPhaseDto getPhaseByCode(String code) {
        log.info("Retrieving SDLC Phase by code: {}", code);
        SDLCPhase phase = sdlcPhaseRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("SDLC Phase not found with code: " + code));

        List<Document> allDocuments = documentRepository.findAll();
        return compilePhaseDto(phase, allDocuments);
    }

    private SdlcPhaseDto compilePhaseDto(SDLCPhase phase, List<Document> allDocuments) {
        List<SdlcPhaseDto.DeliverableMappingDto> mappedDeliverables = new ArrayList<>();

        if (phase.getDeliverables() != null) {
            for (SDLCDeliverable deliv : phase.getDeliverables()) {
                // Find any document associated with this deliverable
                Document matchingDoc = allDocuments.stream()
                        .filter(doc -> doc.getDeliverable() != null && doc.getDeliverable().getId().equals(deliv.getId()))
                        .findFirst()
                        .orElse(null);

                String owner = "N/A";
                String version = "N/A";
                String status = "PENDING_INGESTION";

                if (matchingDoc != null) {
                    owner = matchingDoc.getOwner() != null ? matchingDoc.getOwner().getUsername() : "system";
                    status = matchingDoc.getStatus() != null ? matchingDoc.getStatus().name() : "DRAFT";

                    if (matchingDoc.getVersions() != null && !matchingDoc.getVersions().isEmpty()) {
                        DocumentVersion latest = matchingDoc.getVersions().get(matchingDoc.getVersions().size() - 1);
                        version = latest.getVersionNumber();
                    }
                }

                mappedDeliverables.add(SdlcPhaseDto.DeliverableMappingDto.builder()
                        .deliverableId(deliv.getId())
                        .name(deliv.getName())
                        .code(deliv.getCode())
                        .ownerUsername(owner)
                        .latestVersion(version)
                        .approvalStatus(status)
                        .build());
            }
        }

        return SdlcPhaseDto.builder()
                .id(phase.getId())
                .name(phase.getName())
                .code(phase.getCode())
                .description(phase.getDescription())
                .deliverables(mappedDeliverables)
                .build();
    }
}
