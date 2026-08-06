package com.cth.sdm.application.service;

import com.cth.sdm.application.dto.DocumentStateChangedEvent;
import com.cth.sdm.domain.model.*;
import com.cth.sdm.domain.repository.DocumentRepository;
import com.cth.sdm.domain.repository.ApprovalWorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApprovalProcessEngine {

    private final DocumentRepository documentRepository;
    private final ApprovalWorkflowRepository approvalWorkflowRepository;
    private final VersioningService versioningService;
    private final PdfWatermarkService pdfWatermarkService;
    private final com.cth.sdm.infrastructure.storage.StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    // Direct Integration with embedded Flowable Engine APIs
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    @Transactional
    public ApprovalWorkflow initiateWorkflow(Document document, User maker) {
        log.info("Initiating approval workflow for document: {}", document.getTitle());

        // Create new approval workflow entity
        ApprovalWorkflow workflow = ApprovalWorkflow.builder()
                .document(document)
                .status("PENDING_CHECK") // First step is Checker review
                .initiatedBy(maker)
                .initiatedAt(LocalDateTime.now())
                .steps(new ArrayList<>())
                .histories(new ArrayList<>())
                .build();

        // Log initiation history
        WorkflowHistory history = WorkflowHistory.builder()
                .workflow(workflow)
                .action("INITIATED")
                .actor(maker)
                .actionedAt(LocalDateTime.now())
                .comments("Workflow initiated by Maker")
                .build();
        workflow.getHistories().add(history);

        document.setStatus(DocumentStatus.PENDING_REVIEW);
        documentRepository.save(document);

        ApprovalWorkflow savedWorkflow = approvalWorkflowRepository.save(workflow);

        // TRIGGER FLOWABLE PROCESS INSTANCE
        try {
            runtimeService.startProcessInstanceByKey("documentApproval",
                    Map.of("documentId", document.getId(), "maker", maker.getUsername()));
            log.info("Successfully started Flowable process instance for document approval.");
        } catch (Exception e) {
            log.warn("Flowable process start skipped: {}", e.getMessage());
        }

        eventPublisher.publishEvent(DocumentStateChangedEvent.builder()
                .documentId(document.getId())
                .documentTitle(document.getTitle())
                .newStatus("PENDING_REVIEW")
                .recipientEmail(maker.getEmail())
                .build());

        log.info("Workflow initiated. Current status: PENDING_CHECK");
        return savedWorkflow;
    }

    @Transactional
    public void submit(ApprovalWorkflow workflow, User maker) {
        log.info("Submitting workflow for check stage.");
        workflow.setStatus("PENDING_CHECK");

        WorkflowHistory history = WorkflowHistory.builder()
                .workflow(workflow)
                .action("SUBMIT")
                .actor(maker)
                .actionedAt(LocalDateTime.now())
                .comments("Submitted for Checker review.")
                .build();
        workflow.getHistories().add(history);

        approvalWorkflowRepository.save(workflow);
    }

    @Transactional
    public void approve(ApprovalWorkflow workflow, User actor, String comments) {
        String currentStatus = workflow.getStatus();
        log.info("Approving workflow. Current stage: {}, Actor: {}", currentStatus, actor.getUsername());

        boolean isChecker = actor.getRoles().stream().anyMatch(r -> "ROLE_CHECKER".equals(r.getName()) || "ROLE_ADMIN".equals(r.getName()));
        boolean isApprover = actor.getRoles().stream().anyMatch(r -> "ROLE_APPROVER".equals(r.getName()) || "ROLE_ADMIN".equals(r.getName()));

        // FLOWABLE PROGRAMMATIC TASK COMPLETION
        try {
            Task activeTask = taskService.createTaskQuery()
                    .processVariableValueEquals("documentId", workflow.getDocument().getId())
                    .singleResult();
            if (activeTask != null) {
                taskService.complete(activeTask.getId(), Map.of("approved", true));
                log.info("Successfully completed Flowable workflow task: {}", activeTask.getId());
            }
        } catch (Exception e) {
            log.warn("Flowable task completion skipped: {}", e.getMessage());
        }

        if ("PENDING_CHECK".equalsIgnoreCase(currentStatus)) {
            if (!isChecker) {
                throw new SecurityException("User does not have Checker role permissions.");
            }
            workflow.setStatus("PENDING_APPROVAL");

            WorkflowHistory history = WorkflowHistory.builder()
                    .workflow(workflow)
                    .action("APPROVED_BY_CHECKER")
                    .actor(actor)
                    .actionedAt(LocalDateTime.now())
                    .comments(comments != null ? comments : "Approved by Checker")
                    .build();
            workflow.getHistories().add(history);

            approvalWorkflowRepository.save(workflow);
            log.info("Checker approved. Workflow advanced to: PENDING_APPROVAL");

        } else if ("PENDING_APPROVAL".equalsIgnoreCase(currentStatus)) {
            if (!isApprover) {
                throw new SecurityException("User does not have Approver role permissions.");
            }
            workflow.setStatus("APPROVED");
            workflow.setCompletedAt(LocalDateTime.now());

            WorkflowHistory history = WorkflowHistory.builder()
                    .workflow(workflow)
                    .action("APPROVED_BY_APPROVER")
                    .actor(actor)
                    .actionedAt(LocalDateTime.now())
                    .comments(comments != null ? comments : "Approved by Approver - Published")
                    .build();
            workflow.getHistories().add(history);

            Document document = workflow.getDocument();
            document.setStatus(DocumentStatus.APPROVED);

            // BUMP MAJOR VERSION ON FINAL PUBLISHING/APPROVAL
            if (document.getVersions() != null && !document.getVersions().isEmpty()) {
                DocumentVersion latestVersion = document.getVersions().get(document.getVersions().size() - 1);
                String currentVer = latestVersion.getVersionNumber();
                String bumpedVer = versioningService.bumpMajorVersion(currentVer);
                latestVersion.setVersionNumber(bumpedVer);

                // DIGITAL WATERMARKING upon publishing
                try {
                    String filePath = latestVersion.getFilePath();
                    if (storageService.exists(filePath)) {
                        try (InputStream original = storageService.load(filePath)) {
                            byte[] watermarkedBytes = pdfWatermarkService.addWatermark(original, "APPROVED");

                            // Save watermarked file back to storage
                            storageService.store(filePath, new ByteArrayInputStream(watermarkedBytes),
                                    watermarkedBytes.length, latestVersion.getContentType());
                            log.info("Applied APPROVED digital watermark on file publishing: {}", filePath);
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to apply digital watermark on publication: {}", e.getMessage());
                }
            }

            documentRepository.save(document);
            approvalWorkflowRepository.save(workflow);

            eventPublisher.publishEvent(DocumentStateChangedEvent.builder()
                    .documentId(document.getId())
                    .documentTitle(document.getTitle())
                    .newStatus("APPROVED")
                    .recipientEmail(document.getOwner() != null ? document.getOwner().getEmail() : null)
                    .build());

            log.info("Approver signed off. Workflow completed: APPROVED");
        } else {
            throw new IllegalStateException("Workflow is not in an active approval state.");
        }
    }

    @Transactional
    public void reject(ApprovalWorkflow workflow, User actor, String comments) {
        log.info("Rejecting workflow. Actor: {}", actor.getUsername());
        workflow.setStatus("REJECTED");
        workflow.setCompletedAt(LocalDateTime.now());

        WorkflowHistory history = WorkflowHistory.builder()
                .workflow(workflow)
                .action("REJECTED")
                .actor(actor)
                .actionedAt(LocalDateTime.now())
                .comments(comments != null ? comments : "Rejected")
                .build();
        workflow.getHistories().add(history);

        Document document = workflow.getDocument();
        document.setStatus(DocumentStatus.REJECTED);
        documentRepository.save(document);
        approvalWorkflowRepository.save(workflow);

        eventPublisher.publishEvent(DocumentStateChangedEvent.builder()
                .documentId(document.getId())
                .documentTitle(document.getTitle())
                .newStatus("REJECTED")
                .recipientEmail(document.getOwner() != null ? document.getOwner().getEmail() : null)
                .build());
    }

    @Transactional
    public void requestRevision(ApprovalWorkflow workflow, User actor, String comments) {
        log.info("Requesting revision for workflow. Actor: {}", actor.getUsername());
        workflow.setStatus("REVISION_REQUIRED");

        WorkflowHistory history = WorkflowHistory.builder()
                .workflow(workflow)
                .action("REVISION_REQUESTED")
                .actor(actor)
                .actionedAt(LocalDateTime.now())
                .comments(comments != null ? comments : "Revision requested")
                .build();
        workflow.getHistories().add(history);

        Document document = workflow.getDocument();
        document.setStatus(DocumentStatus.DRAFT);

        // BUMP MINOR VERSION ON REVISION/MODIFICATION
        if (document.getVersions() != null && !document.getVersions().isEmpty()) {
            DocumentVersion latestVersion = document.getVersions().get(document.getVersions().size() - 1);
            String currentVer = latestVersion.getVersionNumber();
            String bumpedVer = versioningService.bumpMinorVersion(currentVer);
            latestVersion.setVersionNumber(bumpedVer);
        }

        documentRepository.save(document);
        approvalWorkflowRepository.save(workflow);

        eventPublisher.publishEvent(DocumentStateChangedEvent.builder()
                .documentId(document.getId())
                .documentTitle(document.getTitle())
                .newStatus("REVISION_REQUIRED")
                .recipientEmail(document.getOwner() != null ? document.getOwner().getEmail() : null)
                .build());
    }
}
