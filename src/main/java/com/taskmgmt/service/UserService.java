package com.taskmgmt.service;

import com.taskmgmt.model.User;
import com.taskmgmt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    @Cacheable(value = "userCache", key = "#id")
    public Optional<User> findById(Long id) {
        log.debug("Fetching user by id: {}", id);
        return userRepository.findById(id);
    }
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public List<User> findAllActiveUsers() {
        return userRepository.findByActiveTrue();
    }
    
    @Transactional
    @CacheEvict(value = "userCache", key = "#user.id")
    public User createUser(String username, String email, String password, String fullName, User.UserRole role) {
        log.info("Creating new user: {}", username);
        
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(hashPassword(password))
                .fullName(fullName)
                .role(role)
                .active(true)
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Side effect: Send notification - might fail but doesn't rollback
        try {
            notificationService.sendWelcomeEmail(savedUser);
        } catch (Exception e) {
            log.error("Failed to send welcome email for user: {}", username, e);
            // Continue anyway - notification failure doesn't prevent user creation
        }
        
        return savedUser;
    }
    
    @Transactional
    @CacheEvict(value = "userCache", key = "#userId")
    public User updateUser(Long userId, String email, String fullName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Email update has side effects - check if new email conflicts
        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(email);
        }
        
        if (fullName != null) {
            user.setFullName(fullName);
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    @Transactional
    @CacheEvict(value = "userCache", key = "#userId")
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        
        // Clear legacy session token
        user.setSessionToken(null);
        
        userRepository.save(user);
        
        // Side effect: Notify user
        try {
            notificationService.sendDeactivationEmail(user);
        } catch (Exception e) {
            log.error("Failed to send deactivation email", e);
        }
    }
    
    private String hashPassword(String password) {
        // TODO: Replace with BCrypt
        return String.valueOf(password.hashCode());
    }
}

