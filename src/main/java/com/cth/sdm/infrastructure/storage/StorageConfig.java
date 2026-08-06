package com.cth.sdm.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@RequiredArgsConstructor
public class StorageConfig {

    private final LocalStorageService localStorageService;
    private final S3StorageService s3StorageService;

    @Value("${app.storage.type:local}")
    private String storageType;

    @Bean
    @Primary
    public StorageService storageService() {
        if ("s3".equalsIgnoreCase(storageType) || "minio".equalsIgnoreCase(storageType)) {
            return s3StorageService;
        }
        return localStorageService;
    }
}
