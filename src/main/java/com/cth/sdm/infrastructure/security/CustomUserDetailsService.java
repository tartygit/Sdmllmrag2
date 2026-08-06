package com.cth.sdm.infrastructure.security;

import com.cth.sdm.domain.model.Role;
import com.cth.sdm.domain.model.User;
import com.cth.sdm.domain.repository.RoleRepository;
import com.cth.sdm.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordPolicyService passwordPolicyService;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOpt = userRepository.findByUsername(username);

        User user;
        if (userOpt.isEmpty()) {
            // JIT (Just-In-Time) User Provisioning for LDAP/Active Directory Authenticated users
            log.info("LDAP/External user '{}' not found in local SQL database. Performing JIT user provisioning...", username);

            // Resolve or create ROLE_VIEWER as default
            Role defaultRole = roleRepository.findByName("ROLE_VIEWER").orElseGet(() -> {
                Role role = Role.builder()
                        .name("ROLE_VIEWER")
                        .description("Default ReadOnly Viewer (JIT Provisioned)")
                        .build();
                try {
                    return roleRepository.save(role);
                } catch (Exception e) {
                    log.warn("Failed to persist default ROLE_VIEWER: {}. Returning transient role.", e.getMessage());
                    return role;
                }
            });

            User newUser = User.builder()
                    .username(username)
                    // Set default secure placeholder password
                    .password("$2a$10$tM2e9.73fF1626jGf9NfHOKO6xG0E8tE7sR7jA.pM.j7t9C9lQ58W")
                    .email(username + "@cth.sdm")
                    .fullName(username + " (JIT Provisioned)")
                    .enabled(true)
                    .passwordUpdatedAt(LocalDateTime.now())
                    .roles(new HashSet<>(Set.of(defaultRole)))
                    .build();

            try {
                user = userRepository.save(newUser);
                log.info("JIT Provisioning completed successfully for user '{}' with default role: ROLE_VIEWER", username);
            } catch (Exception e) {
                log.error("Failed to complete JIT provisioning for user '{}': {}", username, e.getMessage());
                throw new UsernameNotFoundException("JIT provisioning failed for user: " + username, e);
            }
        } else {
            user = userOpt.get();
        }

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .accountExpired(false)
                .credentialsExpired(false)
                .accountLocked(passwordPolicyService.isAccountLocked(user))
                .authorities(authorities)
                .build();
    }
}
