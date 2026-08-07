package com.cth.sdm.infrastructure.security;

import com.cth.sdm.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MfaServiceTest {

    @InjectMocks
    private MfaService mfaService;

    @Test
    void shouldGenerateAndVerifyTotp() {
        ReflectionTestUtils.setField(mfaService, "mfaEnabled", true);
        ReflectionTestUtils.setField(mfaService, "mfaType", "totp");

        String secret = mfaService.generateTotpSecret();
        assertNotNull(secret);
        assertEquals(16, secret.length());

        // For testing, calculate the actual expected TOTP code for the current time index
        long timeIndex = System.currentTimeMillis() / 1000 / 30;
        int expectedCode = mfaService.getTotpForTimeIndex(secret, timeIndex);
        String codeStr = String.format("%06d", expectedCode);

        // Verify the dynamic code
        assertTrue(mfaService.verifyTotp(secret, codeStr));
    }

    @Test
    void shouldGenerateAndVerifyEmailOtp() {
        ReflectionTestUtils.setField(mfaService, "mfaEnabled", true);
        ReflectionTestUtils.setField(mfaService, "mfaType", "email");

        String username = "testuser";
        String otp = mfaService.generateEmailOtp(username);

        assertNotNull(otp);
        assertEquals(6, otp.length());

        assertTrue(mfaService.verifyEmailOtp(username, otp));
        assertFalse(mfaService.verifyEmailOtp(username, otp)); // OTP is one-time use
    }
}
