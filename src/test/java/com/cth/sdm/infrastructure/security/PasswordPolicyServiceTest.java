package com.cth.sdm.infrastructure.security;

import com.cth.sdm.domain.model.User;
import com.cth.sdm.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityAuditService securityAuditService;

    @InjectMocks
    private PasswordPolicyService passwordPolicyService;

    @Test
    void shouldIncrementFailedAttemptsAndLockAccount() {
        ReflectionTestUtils.setField(passwordPolicyService, "failedAttemptsLimit", 3);

        User user = User.builder()
                .username("john_doe")
                .failedAttempts(0)
                .build();

        // Failed login 1
        passwordPolicyService.handleFailedLogin(user);
        assertEquals(1, user.getFailedAttempts());
        assertNull(user.getLockTime());

        // Failed login 2
        passwordPolicyService.handleFailedLogin(user);
        assertEquals(2, user.getFailedAttempts());
        assertNull(user.getLockTime());

        // Failed login 3 -> Lock Limit Reached
        passwordPolicyService.handleFailedLogin(user);
        assertEquals(3, user.getFailedAttempts());
        assertNotNull(user.getLockTime());

        verify(securityAuditService, times(1)).logAccountLockout(eq("john_doe"), anyString(), anyString());
        verify(userRepository, times(3)).save(user);
    }

    @Test
    void shouldResetFailedAttemptsOnSuccessfulLogin() {
        User user = User.builder()
                .username("john_doe")
                .failedAttempts(3)
                .lockTime(LocalDateTime.now())
                .build();

        passwordPolicyService.handleSuccessfulLogin(user);

        assertEquals(0, user.getFailedAttempts());
        assertNull(user.getLockTime());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void shouldCheckAccountLocked() {
        ReflectionTestUtils.setField(passwordPolicyService, "lockDurationMinutes", 15);

        User userNotLocked = User.builder().username("user1").build();
        assertFalse(passwordPolicyService.isAccountLocked(userNotLocked));

        User userLocked = User.builder()
                .username("user2")
                .lockTime(LocalDateTime.now())
                .build();
        assertTrue(passwordPolicyService.isAccountLocked(userLocked));

        User userLockExpired = User.builder()
                .username("user3")
                .lockTime(LocalDateTime.now().minusMinutes(20))
                .build();
        assertFalse(passwordPolicyService.isAccountLocked(userLockExpired));
    }

    @Test
    void shouldCheckPasswordExpired() {
        ReflectionTestUtils.setField(passwordPolicyService, "passwordExpiryDays", 90);

        User userFresh = User.builder()
                .passwordUpdatedAt(LocalDateTime.now().minusDays(30))
                .build();
        assertFalse(passwordPolicyService.isPasswordExpired(userFresh));

        User userExpired = User.builder()
                .passwordUpdatedAt(LocalDateTime.now().minusDays(100))
                .build();
        assertTrue(passwordPolicyService.isPasswordExpired(userExpired));
    }
}
