package com.cth.sdm.service;

import com.cth.sdm.model.User;
import com.cth.sdm.model.Role;
import com.cth.sdm.model.RoleName;
import com.cth.sdm.repository.UserRepository;
import com.cth.sdm.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User createUser(User user, List<String> roleNames) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setPasswordExpiry(LocalDateTime.now().plusDays(90));

        var roles = new HashSet<Role>();
        for (String roleName : roleNames) {
            roleRepository.findByName(RoleName.valueOf(roleName)).ifPresent(roles::add);
        }
        user.setRoles(roles);

        return userRepository.save(user);
    }

    public void lockUser(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLocked(true);
            user.setLockTime(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public void unlockUser(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLocked(false);
            user.setLockTime(null);
            user.setLoginAttempts(0);
            userRepository.save(user);
        });
    }

    public void resetPassword(String username, String newPassword) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setPasswordExpiry(LocalDateTime.now().plusDays(90));
            userRepository.save(user);
        });
    }
}
