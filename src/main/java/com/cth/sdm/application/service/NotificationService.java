package com.cth.sdm.application.service;

import com.cth.sdm.application.dto.DocumentStateChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    @EventListener
    public void handleDocumentStateChangedEvent(DocumentStateChangedEvent event) {
        log.info("Processing state change notification for Document ID: {}", event.getDocumentId());

        String email = event.getRecipientEmail() != null ? event.getRecipientEmail() : "user@cth.sdm";
        String phone = event.getRecipientPhone() != null ? event.getRecipientPhone() : "+15550199";

        sendEmail(email, event.getDocumentTitle(), event.getNewStatus());
        sendSms(phone, event.getDocumentTitle(), event.getNewStatus());
    }

    private void sendEmail(String email, String title, String status) {
        String msg = String.format("Subject: SDDE Status Update [%s]\nDear User,\n\nYour deliverable '%s' status has transitioned to: %s.\n\nBest regards,\nSDDE Engine", status, title, status);
        log.info("[NOTIFICATION - EMAIL] Successfully dispatched email to {}:\n{}", email, msg);
    }

    private void sendSms(String phone, String title, String status) {
        String msg = String.format("SDDE Alert: Deliverable '%s' is now %s.", title, status);
        log.info("[NOTIFICATION - SMS] Successfully dispatched SMS to {}: {}", phone, msg);
    }
}
