package com.cth.sdm.infrastructure.security;

import com.cth.sdm.domain.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MfaService {

    @Value("${app.security.mfa.enabled:false}")
    private boolean mfaEnabled;

    @Value("${app.security.mfa.type:totp}") // totp or email
    private String mfaType;

    // Local transient storage for Email OTP codes
    private final Map<String, String> emailOtpCache = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public String getMfaType() {
        return mfaType;
    }

    public boolean isMfaRequiredForUser(User user) {
        return mfaEnabled && user.isMfaEnabled();
    }

    // --- TOTP Implementation ---

    public String generateTotpSecret() {
        // Generate a 16-character base32-like secret
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public boolean verifyTotp(String secret, String code) {
        if (secret == null || code == null) {
            return false;
        }
        try {
            int codeInt = Integer.parseInt(code.trim());
            long currentTimeIndex = System.currentTimeMillis() / 1000 / 30;

            // Allow window of +/- 1 time step for clock drift
            for (int i = -1; i <= 1; i++) {
                if (getTotpForTimeIndex(secret, currentTimeIndex + i) == codeInt) {
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return false;
    }

    int getTotpForTimeIndex(String secret, long timeIndex) {
        try {
            byte[] keyBytes = decodeBase32(secret);
            byte[] dataBytes = ByteBuffer.allocate(8).putLong(timeIndex).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA1"));
            byte[] hash = mac.doFinal(dataBytes);

            int offset = hash[hash.length - 1] & 0xF;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            return binary % 1000000;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating TOTP: {}", e.getMessage());
            return -1;
        }
    }

    // Custom lightweight Base32 decoder
    private byte[] decodeBase32(String base32) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        base32 = base32.toUpperCase().replaceAll("[^" + chars + "]", "");
        int bits = 5;
        int outputLength = base32.length() * bits / 8;
        byte[] output = new byte[outputLength];

        int buffer = 0;
        int bufferSize = 0;
        int outputIndex = 0;

        for (int i = 0; i < base32.length(); i++) {
            int val = chars.indexOf(base32.charAt(i));
            buffer = (buffer << bits) | val;
            bufferSize += bits;

            if (bufferSize >= 8) {
                bufferSize -= 8;
                if (outputIndex < outputLength) {
                    output[outputIndex++] = (byte) ((buffer >> bufferSize) & 0xFF);
                }
            }
        }
        return output;
    }

    // --- Email OTP Implementation ---

    public String generateEmailOtp(String username) {
        int code = 100000 + secureRandom.nextInt(900000); // 6-digit code
        String codeStr = String.valueOf(code);
        emailOtpCache.put(username, codeStr);

        // Print/log the email OTP code so developers can inspect it in console
        log.info("[MOCK EMAIL OTP] Sent OTP code [{}] to username: {}", codeStr, username);
        return codeStr;
    }

    public boolean verifyEmailOtp(String username, String code) {
        if (username == null || code == null) {
            return false;
        }
        String cachedCode = emailOtpCache.get(username);
        if (cachedCode != null && cachedCode.equals(code.trim())) {
            emailOtpCache.remove(username); // One-time use
            return true;
        }
        return false;
    }
}
