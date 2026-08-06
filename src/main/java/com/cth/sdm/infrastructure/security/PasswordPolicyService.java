package com.cth.sdm.infrastructure.security;

import com.cth.sdm.domain.model.User;
import com.cth.sdm.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordPolicyService {

    private final UserRepository userRepository;
    private final SecurityAuditService securityAuditService;

    @Value("${app.security.password-policy.failed-attempts-limit:5}")
    private int failedAttemptsLimit;

    @Value("${app.security.password-policy.lock-duration-minutes:15}")
    private int lockDurationMinutes;

    @Value("${app.security.password-policy.expiry-days:90}")
    private int passwordExpiryDays;

    @Transactional
    public void handleFailedLogin(User user) {
        int newAttempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(newAttempts);

        if (newAttempts >= failedAttemptsLimit) {
            user.setLockTime(LocalDateTime.now());
            securityAuditService.logAccountLockout(user.getUsername(), "Locked due to " + newAttempts + " failed attempts", "0.0.0.0");
            log.warn("User account [{}] has been locked due to {} failed login attempts.", user.getUsername(), newAttempts);
        }
        userRepository.save(user);
    }

    @Transactional
    public void handleSuccessfulLogin(User user) {
        if (user.getFailedAttempts() > 0 || user.getLockTime() != null) {
            user.setFailedAttempts(0);
            user.setLockTime(null);
            userRepository.save(user);
        }
    }

    public boolean isAccountLocked(User user) {
        if (user.getLockTime() == null) {
            return false;
        }
        LocalDateTime unlockTime = user.getLockTime().plusMinutes(lockDurationMinutes);
        if (LocalDateTime.now().isAfter(unlockTime)) {
            // Lockout duration has expired, implicitly unlocked
            return false;
        }
        return true;
    }

    public boolean isPasswordExpired(User user) {
        if (user.getPasswordUpdatedAt() == null) {
            return false;
        }
        LocalDateTime expiryTime = user.getPasswordUpdatedAt().plusDays(passwordExpiryDays);
        return LocalDateTime.now().isAfter(expiryTime);
    }

    @Transactional
    public void changePassword(User user, String newEncryptedPassword) {
        user.setPassword(newEncryptedPassword);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.setFailedAttempts(0);
        user.setLockTime(null);
        userRepository.save(user);
        securityAuditService.logPasswordReset(user.getUsername(), "0.0.0.0");
    }

    @Transactional
    public void unlockUser(User user) {
        user.setFailedAttempts(0);
        user.setLockTime(null);
        userRepository.save(user);
        securityAuditService.logAdminUnlock(user.getUsername(), "admin", "0.0.0.0");
        log.info("User account [{}] has been administratively unlocked.", user.getUsername());
    }
}
