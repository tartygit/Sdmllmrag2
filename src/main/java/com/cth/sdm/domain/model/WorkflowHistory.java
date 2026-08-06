package com.cth.sdm.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_histories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private ApprovalWorkflow workflow;

    @Column(nullable = false, length = 100)
    private String action; // e.g. SUBMITTED, APPROVED, REJECTED, COMMENTED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Column(name = "actioned_at", nullable = false)
    private LocalDateTime actionedAt;

    @Column(length = 1000)
    private String comments;

    @PrePersist
    protected void onCreate() {
        actionedAt = LocalDateTime.now();
    }
}
