package com.cth.sdm.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service("localStorageService")
@Slf4j
public class LocalStorageService implements StorageService {

    @Value("${app.storage.local-path:/var/data/sdde}")
    private String localPath;

    @Override
    public void store(String path, InputStream stream, long size, String contentType) {
        try {
            Path targetFile = Paths.get(localPath, path);
            Files.createDirectories(targetFile.getParent());
            Files.copy(stream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file locally at: {}", targetFile);
        } catch (IOException e) {
            log.error("Failed to store file locally: {}", e.getMessage());
            throw new RuntimeException("Local storage failed", e);
        }
    }

    @Override
    public InputStream load(String path) {
        try {
            Path file = Paths.get(localPath, path);
            return new FileInputStream(file.toFile());
        } catch (IOException e) {
            log.error("Failed to load file locally: {}", e.getMessage());
            throw new RuntimeException("Local load failed", e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Path file = Paths.get(localPath, path);
            Files.deleteIfExists(file);
            log.info("Deleted file locally at: {}", file);
        } catch (IOException e) {
            log.error("Failed to delete file locally: {}", e.getMessage());
        }
    }

    @Override
    public boolean exists(String path) {
        Path file = Paths.get(localPath, path);
        return Files.exists(file);
    }
}
