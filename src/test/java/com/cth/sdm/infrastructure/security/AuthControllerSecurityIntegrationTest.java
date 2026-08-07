package com.cth.sdm.infrastructure.security;

import com.cth.sdm.application.dto.LoginRequest;
import com.cth.sdm.domain.model.User;
import com.cth.sdm.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Programmatically set the password of the pre-seeded admin user to ensure robust BCrypt matching in tests
        User admin = userRepository.findByUsername("admin")
                .orElseThrow(() -> new AssertionError("Admin user not found"));
        admin.setPassword(passwordEncoder.encode("AdminPassword123!"));
        admin.setLockTime(null);
        admin.setFailedAttempts(0);
        admin.setEnabled(true);
        userRepository.saveAndFlush(admin);
    }

    @Test
    void shouldLoginSuccessfullyWithoutMfa() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("admin")
                .password("AdminPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.mfaRequired", is(false)))
                .andExpect(jsonPath("$.message", containsString("Login successful")));
    }

    @Test
    void shouldFailLoginWithInvalidCredentials() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("admin")
                .password("WrongPassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Invalid username or password")));
    }

    @Test
    void shouldFailLoginWhenAccountIsLocked() throws Exception {
        // Forcefully lock the admin user for testing
        User admin = userRepository.findByUsername("admin")
                .orElseThrow(() -> new AssertionError("Admin not pre-seeded"));
        admin.setLockTime(LocalDateTime.now());
        userRepository.saveAndFlush(admin);

        LoginRequest loginRequest = LoginRequest.builder()
                .username("admin")
                .password("AdminPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.message", containsString("Account is locked")));
    }
}
