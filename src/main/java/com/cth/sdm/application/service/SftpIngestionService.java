package com.cth.sdm.application.service;

import com.cth.sdm.domain.model.User;
import com.cth.sdm.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class SftpIngestionService {

    @Value("${app.ingestion.sftp-enabled:false}")
    private boolean sftpEnabled;

    @Value("${app.ingestion.sftp.host:localhost}")
    private String sftpHost;

    private final IngestionHandler ingestionHandler;
    private final UserRepository userRepository;

    @Scheduled(fixedDelayString = "${app.ingestion.sftp-poll-interval:60000}") // Default 1 minute
    public void pollSftpServer() {
        if (!sftpEnabled) {
            return;
        }

        log.info("Polling SFTP server [host={}] for new deliverables...", sftpHost);

        try {
            // Simulated / Mocked SFTP file retrieval
            // In a real environment, we would connect using JSch (SSH) client, retrieve stream, and ingest.
            User admin = userRepository.findByUsername("admin").orElse(null);

            // Simulating a dummy file retrieval for design demonstration
            String mockContent = "SFTP DELIVERABLE CONTENT WORKFLOW PRE-CHECK";
            InputStream stream = new ByteArrayInputStream(mockContent.getBytes(StandardCharsets.UTF_8));

            log.info("SFTP Ingestion Service picked up file: sftp_mock_document.txt");
            ingestionHandler.ingest(stream, "sftp_mock_document.txt", "text/plain", admin);

        } catch (Exception e) {
            log.error("Failed to poll or process files from SFTP: {}", e.getMessage());
        }
    }
}
