package com.cth.sdm.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    private final JavaMailSender mailSender;
    private final List<String> inAppNotifications = new ArrayList<>();

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@cth.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            // Log fallback or offline mode gracefully without throwing exceptions
            System.err.println("Email Delivery Offline: " + e.getMessage());
        }
    }

    public void sendSMS(String phoneNumber, String message) {
        // SMS Gateway API endpoint driver simulation
        System.out.println("Dispatching SMS alert to " + phoneNumber + ": " + message);
    }

    public void publishInAppAlert(String alert) {
        inAppNotifications.add(alert);
        System.out.println("New In-App notification: " + alert);
    }

    public List<String> getInAppNotifications() {
        return inAppNotifications;
    }
}
