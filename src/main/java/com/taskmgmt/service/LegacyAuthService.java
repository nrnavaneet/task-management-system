package com.taskmgmt.service;

import com.taskmgmt.model.User;
import com.taskmgmt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Legacy authentication service.
 * TODO: Remove after migration to JWT-only system (target: Q2 2024)
 * 
 * This service maintains session tokens in the database.
 * Deprecated in favor of stateless JWT tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LegacyAuthService {
    private final UserRepository userRepository;
    
    /**
     * Generates a legacy session token and stores it in user record.
     * This creates stateful sessions - different from JWT approach.
     */
    @Transactional
    public String generateLegacyToken(User user) {
        String token = UUID.randomUUID().toString();
        user.setSessionToken(token);
        userRepository.save(user);
        log.debug("Generated legacy token for user: {}", user.getUsername());
        return token;
    }
    
    /**
     * Validates legacy token by looking up user in database.
     * Note: This requires database lookup on every request.
     */
    public Optional<User> validateLegacyToken(String token) {
        Optional<User> user = userRepository.findBySessionToken(token);
        if (user.isPresent() && user.get().getActive()) {
            // Update last login on token validation - side effect
            user.get().setLastLoginAt(LocalDateTime.now());
            userRepository.save(user.get());
            return user;
        }
        return Optional.empty();
    }
    
    /**
     * Invalidates legacy session token.
     * Called on logout.
     */
    @Transactional
    public void invalidateToken(String token) {
        userRepository.findBySessionToken(token).ifPresent(user -> {
            user.setSessionToken(null);
            userRepository.save(user);
        });
    }
}

