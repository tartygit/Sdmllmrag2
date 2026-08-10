package com.cth.sdm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileInputStream;

@Service
public class FolderPickupHandler {

    private final DocumentService documentService;
    private final String watchDirectory;
    private final boolean enabled;

    public FolderPickupHandler(
            DocumentService documentService,
            @Value("${app.pickup.dir:/tmp/sdde_pickup}") String watchDirectory,
            @Value("${app.pickup.enabled:false}") boolean enabled) {
        this.documentService = documentService;
        this.watchDirectory = watchDirectory;
        this.enabled = enabled;

        File watchDir = new File(watchDirectory);
        if (!watchDir.exists()) {
            watchDir.mkdirs();
        }
    }

    @Scheduled(fixedDelay = 15000) // Poll watch directory folder every 15 seconds
    public void scanAndIngest() {
        if (!enabled) {
            return;
        }

        File folder = new File(watchDirectory);
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isFile()) {
                try {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        documentService.uploadDocument(
                                fis,
                                file.getName(),
                                file.length(),
                                "SYS", // Default system-pickup application code
                                1, // Default to requirements phase 1
                                "system_poller",
                                "Automatically ingested deliverable document via local folder watcher handler."
                        );
                    }
                    // Delete processed file after successful workflow ingestion
                    file.delete();
                } catch (Exception e) {
                    System.err.println("Folder poller ingestion error for: " + file.getName() + " | " + e.getMessage());
                }
            }
        }
    }
}
