package com.cth.sdm.infrastructure.security;

import com.cth.sdm.domain.model.Permission;
import com.cth.sdm.domain.model.Role;
import com.cth.sdm.domain.model.User;
import com.cth.sdm.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final UserRepository userRepository;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || targetDomainObject == null || permission == null) {
            return false;
        }

        String username = authentication.getName();
        String resource = targetDomainObject.toString().toUpperCase();
        String action = permission.toString().toUpperCase();

        log.debug("Checking dynamic permission for user [{}]: resource={}, action={}", username, resource, action);

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // 1. Role ROLE_ADMIN has absolute bypass/access to all features
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equalsIgnoreCase(role.getName()));
        if (isAdmin) {
            return true;
        }

        // 2. Check if any of the user's roles contain the required permission
        // Required permission format: "RESOURCE:ACTION" e.g., "DOCUMENT:WRITE"
        String requiredPermissionStr = resource + ":" + action;

        for (Role role : user.getRoles()) {
            if (role.getPermissions() != null) {
                for (Permission perm : role.getPermissions()) {
                    if (requiredPermissionStr.equalsIgnoreCase(perm.getName())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        // Simple delegator to the main implementation or return hasPermission by targetType and action
        return hasPermission(authentication, targetType, permission);
    }
}
