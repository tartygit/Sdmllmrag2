package com.cth.sdm.infrastructure.storage;

import com.cth.sdm.application.dto.LoginRequest;
import com.cth.sdm.domain.model.Document;
import com.cth.sdm.domain.repository.DocumentRepository;
import com.cth.sdm.infrastructure.messaging.RabbitEventDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DocumentIngestionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private StorageService storageService; // Will resolve to LocalStorageService in tests

    @MockBean
    private RabbitEventDispatcher rabbitEventDispatcher;

    @BeforeEach
    void setUp() {
        // Clear documents before each test
        documentRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldUploadStoreAndValidateDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "clean_srs_document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%%PDF-1.4 - MOCK PDF SRS CONTENT".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ingested successfully")));

        // Verify the document entity has been saved to DB
        java.util.List<Document> docs = documentRepository.findAll();
        assertFalse(docs.isEmpty());
        Document savedDoc = docs.stream()
                .filter(d -> "clean_srs_document.pdf".equals(d.getTitle()))
                .findFirst()
                .orElse(null);

        assertNotNull(savedDoc);
        assertEquals("clean_srs_document.pdf", savedDoc.getTitle());
        assertFalse(savedDoc.getVersions().isEmpty());

        // Verify local storage service has saved the file correctly
        String filePath = savedDoc.getVersions().get(0).getFilePath();
        assertTrue(storageService.exists(filePath));

        try (InputStream loadedStream = storageService.load(filePath)) {
            String loadedContent = new String(loadedStream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(loadedContent.contains("%%PDF-1.4"));
        }

        // Verify event dispatcher was called to publish the ingestion event
        verify(rabbitEventDispatcher, times(1)).dispatchDocumentIngestedEvent(any());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldBlockAndQuarantineInfectedUpload() throws Exception {
        // Upload the standard EICAR anti-virus test signature as payload
        String eicarPayload = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "infected_threat.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                eicarPayload.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Security threat detected")));

        // Verify nothing was persisted
        java.util.List<Document> docs = documentRepository.findAll();
        assertTrue(docs.isEmpty());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldBlockUnsupportedExtensions() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious_executable.exe",
                "application/octet-stream",
                "MOCK EXEC".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Unsupported file format")));

        // Verify nothing was persisted
        java.util.List<Document> docs = documentRepository.findAll();
        assertTrue(docs.isEmpty());
    }
}
