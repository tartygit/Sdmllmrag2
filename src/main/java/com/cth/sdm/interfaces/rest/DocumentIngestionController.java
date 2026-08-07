package com.cth.sdm.interfaces.rest;

import com.cth.sdm.application.service.DocumentIngestionService;
import com.cth.sdm.domain.model.User;
import com.cth.sdm.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentIngestionController {

    private final DocumentIngestionService documentIngestionService;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty.");
        }

        String originalFileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        log.info("Received REST deliverable ingestion request: {}", originalFileName);

        try {
            // Retrieve current authenticated user from SecurityContext
            String username = SecurityContextHolder.getContext().getAuthentication() != null ?
                    SecurityContextHolder.getContext().getAuthentication().getName() : "admin";

            User currentUser = userRepository.findByUsername(username)
                    .orElseGet(() -> userRepository.findByUsername("admin").orElse(null));

            try (InputStream stream = file.getInputStream()) {
                documentIngestionService.ingest(stream, originalFileName, contentType, currentUser);
            }

            return ResponseEntity.ok("File '" + originalFileName + "' ingested successfully.");
        } catch (SecurityException e) {
            log.error("Antivirus scan flagged the file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Security threat detected! Ingestion rejected.");
        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to ingest file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ingestion failed: " + e.getMessage());
        }
    }
}
