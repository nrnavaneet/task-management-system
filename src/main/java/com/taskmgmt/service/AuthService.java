package com.taskmgmt.service;

import com.taskmgmt.model.User;
import com.taskmgmt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final LegacyAuthService legacyAuthService; // Legacy auth - still in use
    
    @Value("${legacy.auth.enabled:true}")
    private boolean legacyAuthEnabled;
    
    /**
     * Authenticates user and returns token.
     * Uses new JWT system by default, falls back to legacy if enabled.
     */
    @Transactional
    public AuthResult authenticate(String username, String password) {
        log.info("Authenticating user: {}", username);
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return AuthResult.failure("Invalid credentials");
        }
        
        User user = userOpt.get();
        if (!user.getActive()) {
            return AuthResult.failure("User account is inactive");
        }
        
        // Password validation - multiple paths
        boolean passwordValid = validatePassword(user, password);
        if (!passwordValid) {
            return AuthResult.failure("Invalid credentials");
        }
        
        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Token generation - dual system
        String token;
        if (legacyAuthEnabled && isLegacyUser(user)) {
            token = legacyAuthService.generateLegacyToken(user);
            log.warn("Using legacy auth for user: {}", username);
        } else {
            token = jwtTokenService.generateToken(user);
        }
        
        return AuthResult.success(token, user);
    }
    
    /**
     * Validates password - might have multiple implementations
     */
    private boolean validatePassword(User user, String password) {
        // Simple hash comparison - production would use BCrypt
        String hash = hashPassword(password);
        return user.getPasswordHash().equals(hash);
    }
    
    private String hashPassword(String password) {
        // TODO: Replace with proper BCrypt hashing
        return String.valueOf(password.hashCode());
    }
    
    private boolean isLegacyUser(User user) {
        // Legacy users have sessionToken set - ambiguous logic
        return user.getSessionToken() != null && !user.getSessionToken().isEmpty();
    }
    
    @Cacheable(value = "userCache", key = "#token")
    public Optional<User> validateToken(String token) {
        // Try legacy first if enabled (it returns User directly)
        if (legacyAuthEnabled) {
            Optional<User> legacyUser = legacyAuthService.validateLegacyToken(token);
            if (legacyUser.isPresent()) {
                return legacyUser;
            }
        }
        
        // Try JWT - need to fetch user after validation
        try {
            String username = jwtTokenService.getUsernameFromToken(token);
            return userRepository.findByUsername(username);
        } catch (Exception e) {
            log.debug("JWT token validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    public String getUsernameFromToken(String token) {
        try {
            return jwtTokenService.getUsernameFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static class AuthResult {
        private final boolean success;
        private final String token;
        private final User user;
        private final String error;
        
        private AuthResult(boolean success, String token, User user, String error) {
            this.success = success;
            this.token = token;
            this.user = user;
            this.error = error;
        }
        
        public static AuthResult success(String token, User user) {
            return new AuthResult(true, token, user, null);
        }
        
        public static AuthResult failure(String error) {
            return new AuthResult(false, null, null, error);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getToken() { return token; }
        public User getUser() { return user; }
        public String getError() { return error; }
    }
}

