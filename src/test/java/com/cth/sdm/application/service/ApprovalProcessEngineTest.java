package com.cth.sdm.application.service;

import com.cth.sdm.domain.model.*;
import com.cth.sdm.domain.repository.DocumentRepository;
import com.cth.sdm.domain.repository.ApprovalWorkflowRepository;
import com.cth.sdm.infrastructure.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalProcessEngineTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ApprovalWorkflowRepository approvalWorkflowRepository;

    @Mock
    private VersioningService versioningService;

    @Mock
    private PdfWatermarkService pdfWatermarkService;

    @Mock
    private StorageService storageService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ApprovalProcessEngine approvalProcessEngine;

    @Test
    void shouldTransitionThroughFullWorkflowSuccessfully() {
        Role roleChecker = Role.builder().name("ROLE_CHECKER").build();
        Role roleApprover = Role.builder().name("ROLE_APPROVER").build();

        User maker = User.builder().username("maker").email("maker@cth.sdm").build();
        User checker = User.builder().username("checker").roles(Set.of(roleChecker)).build();
        User approver = User.builder().username("approver").roles(Set.of(roleApprover)).build();

        DocumentVersion version = DocumentVersion.builder()
                .versionNumber("1.0.0")
                .filePath("documents/spec.pdf")
                .contentType("application/pdf")
                .build();

        Document document = Document.builder()
                .id(1L)
                .title("Deliverable.pdf")
                .status(DocumentStatus.DRAFT)
                .owner(maker)
                .versions(new ArrayList<>(java.util.List.of(version)))
                .build();

        // Setup mock save behavior
        when(approvalWorkflowRepository.save(any(ApprovalWorkflow.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 1. Initiate Workflow
        ApprovalWorkflow workflow = approvalProcessEngine.initiateWorkflow(document, maker);

        assertNotNull(workflow);
        assertEquals("PENDING_CHECK", workflow.getStatus());
        assertEquals(DocumentStatus.PENDING_REVIEW, document.getStatus());
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));

        // 2. Approve by Checker -> PENDING_APPROVAL
        approvalProcessEngine.approve(workflow, checker, "Checked and looks solid");
        assertEquals("PENDING_APPROVAL", workflow.getStatus());

        // 3. Approve by Approver -> APPROVED (bumb major version & watermark)
        when(versioningService.bumpMajorVersion("1.0.0")).thenReturn("2.0.0");
        when(storageService.exists("documents/spec.pdf")).thenReturn(true);
        when(storageService.load("documents/spec.pdf")).thenReturn(mock(java.io.InputStream.class));
        when(pdfWatermarkService.addWatermark(any(), eq("APPROVED"))).thenReturn("WATERMARKED_BYTES".getBytes());

        approvalProcessEngine.approve(workflow, approver, "Approved for release");

        assertEquals("APPROVED", workflow.getStatus());
        assertEquals(DocumentStatus.APPROVED, document.getStatus());
        assertEquals("2.0.0", version.getVersionNumber());

        verify(storageService, times(1)).store(eq("documents/spec.pdf"), any(), eq(17L), eq("application/pdf"));
        verify(eventPublisher, times(2)).publishEvent(any(Object.class));
    }

    @Test
    void shouldRejectWorkflow() {
        Role roleChecker = Role.builder().name("ROLE_CHECKER").build();
        User maker = User.builder().username("maker").build();
        User checker = User.builder().username("checker").roles(Set.of(roleChecker)).build();

        Document document = Document.builder()
                .id(1L)
                .title("Deliverable.pdf")
                .status(DocumentStatus.PENDING_REVIEW)
                .build();

        ApprovalWorkflow workflow = ApprovalWorkflow.builder()
                .document(document)
                .status("PENDING_CHECK")
                .build();

        // Setup mock save behavior
        when(approvalWorkflowRepository.save(any(ApprovalWorkflow.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        approvalProcessEngine.reject(workflow, checker, "Rejection feedback comment");

        assertEquals("REJECTED", workflow.getStatus());
        assertEquals(DocumentStatus.REJECTED, document.getStatus());
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
    }
}
