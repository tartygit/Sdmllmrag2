package com.cth.sdm.application.service;

import com.cth.sdm.domain.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class AutoNumberingService {

    @Value("${spring.application.code:SDDE}")
    private String defaultAppCode;

    private final DocumentRepository documentRepository;

    // In-memory sequence fallback to ensure smooth operation if database sequence is not used
    private final Map<String, AtomicLong> sequenceMap = new ConcurrentHashMap<>();

    public synchronized String generateDocumentNumber(String appCode, String phaseCode) {
        String activeAppCode = (appCode != null && !appCode.isBlank()) ? appCode.toUpperCase() : defaultAppCode;
        String activePhaseCode = (phaseCode != null && !phaseCode.isBlank()) ? phaseCode.toUpperCase() : "PHASE";

        String key = activeAppCode + "-" + activePhaseCode;

        // Compute next sequence number by querying count or utilizing atomically incremented in-memory maps
        long currentCount = documentRepository.count();
        long nextSequence = sequenceMap.computeIfAbsent(key, k -> new AtomicLong(currentCount)).incrementAndGet();

        String formattedSequence = String.format("%03d", nextSequence);
        String generatedId = activeAppCode + "-" + activePhaseCode + "-" + formattedSequence;

        log.info("Auto-generated Document Number: {}", generatedId);
        return generatedId;
    }
}
