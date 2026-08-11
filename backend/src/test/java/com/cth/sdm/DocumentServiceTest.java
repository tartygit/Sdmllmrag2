package com.cth.sdm;

import com.cth.sdm.model.Document;
import com.cth.sdm.model.DocumentStatus;
import com.cth.sdm.repository.AIRecommendationRepository;
import com.cth.sdm.repository.DocumentRepository;
import com.cth.sdm.repository.DocumentWorkflowRepository;
import com.cth.sdm.service.AIAgentOrchestrator;
import com.cth.sdm.service.DocumentService;
import com.cth.sdm.util.ClamAVScanner;
import com.cth.sdm.util.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class DocumentServiceTest {

    private DocumentRepository documentRepository;
    private DocumentWorkflowRepository workflowRepository;
    private StorageService storageService;
    private ClamAVScanner clamAVScanner;
    private AIAgentOrchestrator aiAgentOrchestrator;
    private AIRecommendationRepository aiRecommendationRepository;
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentRepository = Mockito.mock(DocumentRepository.class);
        workflowRepository = Mockito.mock(DocumentWorkflowRepository.class);
        storageService = Mockito.mock(StorageService.class);
        clamAVScanner = Mockito.mock(ClamAVScanner.class);
        aiAgentOrchestrator = Mockito.mock(AIAgentOrchestrator.class);
        aiRecommendationRepository = Mockito.mock(AIRecommendationRepository.class);

        documentService = new DocumentService(
                documentRepository,
                workflowRepository,
                storageService,
                clamAVScanner,
                aiAgentOrchestrator,
                aiRecommendationRepository
        );
    }

    @Test
    void testUploadDocumentSuccess() {
        when(storageService.storeFile(any(), anyString())).thenReturn("/tmp/path.docx");
        when(clamAVScanner.isSafe(anyString())).thenReturn(true);

        Document mockDoc = Document.builder()
                .id(1L)
                .documentCode("PAY-P101")
                .name("test.docx")
                .fileType("DOCX")
                .status(DocumentStatus.INDEXED)
                .build();

        when(documentRepository.save(any())).thenReturn(mockDoc);

        ByteArrayInputStream is = new ByteArrayInputStream("content".getBytes());
        Document doc = documentService.uploadDocument(
                is, "test.docx", 7, "PAY", 1, "jules", "description"
        );

        assertNotNull(doc);
        assertEquals(DocumentStatus.INDEXED, doc.getStatus());
    }

    @Test
    void testWorkflowTransitions() {
        Document mockDoc = Document.builder()
                .id(1L)
                .documentCode("PAY-P101")
                .name("test.docx")
                .fileType("DOCX")
                .status(DocumentStatus.INDEXED)
                .build();

        when(documentRepository.findById(1L)).thenReturn(Optional.of(mockDoc));
        when(documentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Document pendingDoc = documentService.submitForApproval(1L, "jules");
        assertEquals(DocumentStatus.PENDING_APPROVAL, pendingDoc.getStatus());

        Document approvedDoc = documentService.approveDocument(1L, "admin", "Looks great!");
        assertEquals(DocumentStatus.APPROVED, approvedDoc.getStatus());
    }
}
