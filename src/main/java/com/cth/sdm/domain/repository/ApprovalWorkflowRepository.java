package com.cth.sdm.domain.repository;

import com.cth.sdm.domain.model.ApprovalWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long> {
}
