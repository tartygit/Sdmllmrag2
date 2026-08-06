package com.cth.sdm.infrastructure.security;

import com.cth.sdm.domain.model.Permission;
import com.cth.sdm.domain.model.Role;
import com.cth.sdm.domain.model.User;
import com.cth.sdm.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomPermissionEvaluatorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CustomPermissionEvaluator customPermissionEvaluator;

    @Test
    void adminShouldHaveAllPermissions() {
        when(authentication.getName()).thenReturn("admin");

        Role adminRole = Role.builder().name("ROLE_ADMIN").build();
        User adminUser = User.builder()
                .username("admin")
                .roles(Set.of(adminRole))
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        boolean hasPerm = customPermissionEvaluator.hasPermission(authentication, "DOCUMENT", "WRITE");
        assertTrue(hasPerm);
    }

    @Test
    void userShouldHaveDesignatedPermissions() {
        when(authentication.getName()).thenReturn("maker");

        Permission writePerm = Permission.builder().name("DOCUMENT:WRITE").build();
        Role makerRole = Role.builder()
                .name("ROLE_MAKER")
                .permissions(Set.of(writePerm))
                .build();

        User makerUser = User.builder()
                .username("maker")
                .roles(Set.of(makerRole))
                .build();

        when(userRepository.findByUsername("maker")).thenReturn(Optional.of(makerUser));

        // Has permission to write documents
        assertTrue(customPermissionEvaluator.hasPermission(authentication, "DOCUMENT", "WRITE"));

        // Does NOT have permission to delete documents
        assertFalse(customPermissionEvaluator.hasPermission(authentication, "DOCUMENT", "DELETE"));
    }
}
