package com.cth.sdm.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VersioningService {

    public String bumpMinorVersion(String currentVersion) {
        if (currentVersion == null || currentVersion.isBlank()) {
            return "1.1.0";
        }

        try {
            String[] parts = currentVersion.split("\\.");
            if (parts.length >= 2) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]) + 1;
                int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                return major + "." + minor + "." + patch;
            } else {
                int major = Integer.parseInt(parts[0]);
                return major + ".1.0";
            }
        } catch (Exception e) {
            log.warn("Failed to parse version '{}', defaulting to bumped minor '1.1.0'", currentVersion);
            return "1.1.0";
        }
    }

    public String bumpMajorVersion(String currentVersion) {
        if (currentVersion == null || currentVersion.isBlank()) {
            return "2.0.0";
        }

        try {
            String[] parts = currentVersion.split("\\.");
            int major = Integer.parseInt(parts[0]) + 1;
            return major + ".0.0";
        } catch (Exception e) {
            log.warn("Failed to parse version '{}', defaulting to bumped major '2.0.0'", currentVersion);
            return "2.0.0";
        }
    }
}
