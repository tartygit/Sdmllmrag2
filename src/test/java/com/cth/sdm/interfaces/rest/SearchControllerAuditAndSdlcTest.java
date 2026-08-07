package com.cth.sdm.interfaces.rest;

import com.cth.sdm.domain.model.*;
import com.cth.sdm.domain.repository.AuditLogRepository;
import com.cth.sdm.domain.repository.DocumentRepository;
import com.cth.sdm.domain.repository.SDLCDeliverableRepository;
import com.cth.sdm.domain.repository.SDLCPhaseRepository;
import com.cth.sdm.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SearchControllerAuditAndSdlcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private SDLCPhaseRepository sdlcPhaseRepository;

    @Autowired
    private SDLCDeliverableRepository sdlcDeliverableRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private User admin;
    private SDLCPhase reqPhase;
    private SDLCDeliverable srsDeliverable;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        auditLogRepository.deleteAll();

        // 1. Get pre-seeded admin user
        admin = userRepository.findByUsername("admin")
                .orElseThrow(() -> new AssertionError("Admin not pre-seeded"));

        // 2. Create SDLC Phase
        reqPhase = SDLCPhase.builder()
                .name("Requirements Phase")
                .code("REQ")
                .description("Gathering requirements")
                .build();
        reqPhase = sdlcPhaseRepository.saveAndFlush(reqPhase);

        // 3. Create SDLC Deliverable
        srsDeliverable = SDLCDeliverable.builder()
                .name("Software Requirements Specification")
                .code("SRS")
                .description("SRS deliverable")
                .phase(reqPhase)
                .build();
        srsDeliverable = sdlcDeliverableRepository.saveAndFlush(srsDeliverable);

        // Synchronize JPA parent-child relationship in-memory with a modifiable Set
        reqPhase.setDeliverables(new java.util.HashSet<>(java.util.Set.of(srsDeliverable)));
        sdlcPhaseRepository.saveAndFlush(reqPhase);

        // 4. Ingest/Create Document
        Document doc = Document.builder()
                .title("PAY-REQ-001_srs_specification.pdf")
                .documentNumber("PAY-REQ-001")
                .description("Core payment engine requirements specification document")
                .status(DocumentStatus.DRAFT)
                .owner(admin)
                .deliverable(srsDeliverable)
                .build();

        DocumentVersion version = DocumentVersion.builder()
                .document(doc)
                .versionNumber("1.0.0")
                .filePath("documents/PAY-REQ-001.pdf")
                .createdBy(admin)
                .build();

        doc.setVersions(List.of(version));
        documentRepository.saveAndFlush(doc);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldSearchWithMultiFieldFilters() throws Exception {
        // Multi-field search filtering by keyword, phase, appCode, and status
        mockMvc.perform(get("/api/search")
                        .param("query", "payment")
                        .param("phaseCode", "REQ")
                        .param("appCode", "PAY")
                        .param("status", "DRAFT")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", containsString("srs_specification")))
                .andExpect(jsonPath("$[0].appCode", is("PAY")))
                .andExpect(jsonPath("$[0].phaseCode", is("REQ")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldTriggerAopAuditLogOnSearch() throws Exception {
        // Perform search request
        mockMvc.perform(get("/api/search")
                        .param("query", "payment")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Assert that the AOP Aspect intercepted the call and persisted a new AuditLog record in database!
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertFalse(auditLogs.isEmpty());

        AuditLog searchAudit = auditLogs.stream()
                .filter(log -> "DOCUMENT_SEARCH".equals(log.getAction()))
                .findFirst()
                .orElse(null);

        assertNotNull(searchAudit);
        assertEquals("admin", searchAudit.getUsername());
        assertEquals("DOCUMENT_SEARCH", searchAudit.getAction());
        assertThat(searchAudit.getDetails(), containsString("query=payment"));
    }

    private void assertNotNull(Object obj) {
        org.junit.jupiter.api.Assertions.assertNotNull(obj);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldRetrieveSdlcPhasesWithMappedDeliverables() throws Exception {
        mockMvc.perform(get("/api/sdlc/phases")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[*].code", hasItem("REQ")))
                .andExpect(jsonPath("$[0].deliverables", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].deliverables[0].code", is("SRS")))
                .andExpect(jsonPath("$[0].deliverables[0].latestVersion", is("1.0.0"))); // fallback default version
    }
}
