package com.cth.sdm.application.service;

import com.cth.sdm.domain.model.User;
import com.cth.sdm.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DirectoryWatcherService {

    @Value("${app.ingestion.local-watch-dir:/tmp/sdde-pickup}")
    private String watchDir;

    @Value("${app.ingestion.watch-enabled:false}")
    private boolean watchEnabled;

    private final IngestionHandler ingestionHandler;
    private final UserRepository userRepository;
    private ExecutorService executorService;
    private WatchService watchService;

    @PostConstruct
    public void startWatching() {
        if (!watchEnabled) {
            log.info("Directory watcher is disabled.");
            return;
        }

        try {
            Path path = Paths.get(watchDir);
            Files.createDirectories(path);

            this.watchService = FileSystems.getDefault().newWatchService();
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            this.executorService = Executors.newSingleThreadExecutor();
            this.executorService.submit(this::watchLoop);
            log.info("Started watching directory: {}", watchDir);
        } catch (IOException e) {
            log.error("Failed to start directory watcher: {}", e.getMessage());
        }
    }

    private void watchLoop() {
        try {
            User defaultAdmin = userRepository.findByUsername("admin").orElse(null);

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path filename = (Path) event.context();
                        Path child = Paths.get(watchDir).resolve(filename);
                        log.info("New file detected for pickup: {}", child);

                        // Give it a brief moment to finish writing
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }

                        if (Files.exists(child)) {
                            try (FileInputStream fis = new FileInputStream(child.toFile())) {
                                String contentType = Files.probeContentType(child);
                                if (contentType == null) {
                                    contentType = "application/octet-stream";
                                }
                                ingestionHandler.ingest(fis, filename.toString(), contentType, defaultAdmin);
                                // Delete the file after ingestion
                                Files.delete(child);
                                log.info("Successfully processed and deleted pickup file: {}", child);
                            } catch (Exception e) {
                                log.error("Failed to ingest pickup file [{}]: {}", child, e.getMessage());
                            }
                        }
                    }
                }
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            log.info("Directory watcher interrupted.");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Directory watcher loop error: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stopWatching() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.error("Failed to close WatchService: {}", e.getMessage());
            }
        }
    }
}
