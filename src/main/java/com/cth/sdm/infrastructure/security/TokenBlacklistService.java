package com.cth.sdm.infrastructure.security;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    // Map of token to its expiration timestamp in milliseconds
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void blacklistToken(String token, long expirationTimeMillis) {
        blacklist.put(token, expirationTimeMillis);
    }

    public boolean isBlacklisted(String token) {
        Long expiration = blacklist.get(token);
        if (expiration == null) {
            return false;
        }
        if (expiration < System.currentTimeMillis()) {
            blacklist.remove(token); // Cleanup expired entry
            return false;
        }
        return true;
    }
}
