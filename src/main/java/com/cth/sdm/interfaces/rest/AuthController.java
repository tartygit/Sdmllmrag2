package com.cth.sdm.interfaces.rest;

import com.cth.sdm.application.dto.*;
import com.cth.sdm.domain.model.User;
import com.cth.sdm.domain.repository.UserRepository;
import com.cth.sdm.infrastructure.security.JwtTokenProvider;
import com.cth.sdm.infrastructure.security.MfaService;
import com.cth.sdm.infrastructure.security.PasswordPolicyService;
import com.cth.sdm.infrastructure.security.TokenBlacklistService;
import com.cth.sdm.infrastructure.security.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordPolicyService passwordPolicyService;
    private final MfaService mfaService;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditService securityAuditService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        // 1. Retrieve user from db to apply password policies (if local database auth)
        java.util.Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Check if account is currently locked
            if (passwordPolicyService.isAccountLocked(user)) {
                securityAuditService.logLoginFailure(username, "Account is locked", "0.0.0.0");
                return ResponseEntity.status(HttpStatus.LOCKED)
                        .body(LoginResponse.builder()
                                .message("Account is locked due to too many failed attempts. Try again later.")
                                .build());
            }

            // Check if password matches
            if (!passwordEncoder.matches(password, user.getPassword())) {
                securityAuditService.logLoginFailure(username, "Invalid password credentials", "0.0.0.0");
                passwordPolicyService.handleFailedLogin(user);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(LoginResponse.builder().message("Invalid username or password.").build());
            }

            // Check if password has expired
            if (passwordPolicyService.isPasswordExpired(user)) {
                securityAuditService.logLoginFailure(username, "Password expired", "0.0.0.0");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(LoginResponse.builder()
                                .message("Your password has expired. Please reset your password.")
                                .build());
            }
        }

        // 2. Perform authentication with AuthenticationManager
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException e) {
            securityAuditService.logLoginFailure(username, "Authentication failed", "0.0.0.0");
            log.error("Authentication failed for user: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LoginResponse.builder().message("Invalid username or password.").build());
        }

        // 3. If authentication is successful, check MFA requirements
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            passwordPolicyService.handleSuccessfulLogin(user);

            if (mfaService.isMfaRequiredForUser(user)) {
                String mfaType = mfaService.getMfaType();
                if ("email".equalsIgnoreCase(mfaType)) {
                    mfaService.generateEmailOtp(username);
                }
                return ResponseEntity.ok(LoginResponse.builder()
                        .mfaRequired(true)
                        .mfaType(mfaType)
                        .message("MFA verification is required to complete login.")
                        .build());
            }
        }

        // 4. Generate Tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String accessToken = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        securityAuditService.logLoginSuccess(username, "0.0.0.0");

        return ResponseEntity.ok(LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .message("Login successful.")
                .build());
    }

    @PostMapping("/mfa-verify")
    public ResponseEntity<LoginResponse> verifyMfa(@RequestBody MfaVerifyRequest request) {
        String username = request.getUsername();
        String code = request.getCode();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        boolean isValid = false;
        String mfaType = mfaService.getMfaType();
        if ("totp".equalsIgnoreCase(mfaType)) {
            isValid = mfaService.verifyTotp(user.getMfaSecret(), code);
        } else if ("email".equalsIgnoreCase(mfaType)) {
            isValid = mfaService.verifyEmailOtp(username, code);
        }

        if (!isValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LoginResponse.builder().message("Invalid MFA verification code.").build());
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String accessToken = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        return ResponseEntity.ok(LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .message("MFA Verification successful.")
                .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        if (jwtTokenProvider.isTokenValid(refreshToken)) {
            String username = jwtTokenProvider.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            String newAccessToken = jwtTokenProvider.generateToken(userDetails);
            return ResponseEntity.ok(LoginResponse.builder()
                    .token(newAccessToken)
                    .refreshToken(refreshToken)
                    .message("Token refreshed successfully.")
                    .build());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(LoginResponse.builder().message("Invalid or expired refresh token.").build());
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                java.util.Date exp = jwtTokenProvider.extractExpiration(token);
                tokenBlacklistService.blacklistToken(token, exp.getTime());
                return ResponseEntity.ok("Logged out successfully.");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error processing logout.");
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Authorization header is missing or malformed.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody PasswordResetRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or current password.");
        }

        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
        passwordPolicyService.changePassword(user, encodedNewPassword);

        return ResponseEntity.ok("Password has been reset successfully.");
    }

    @PostMapping("/unlock")
    public ResponseEntity<String> unlock(@RequestBody UnlockRequest request) {
        // Administrative unlock endpoint (ideally protected by ADMIN role)
        User user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        passwordPolicyService.unlockUser(user);
        return ResponseEntity.ok("User account has been successfully unlocked.");
    }
}
