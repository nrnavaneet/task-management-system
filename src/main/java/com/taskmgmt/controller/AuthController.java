package com.taskmgmt.controller;

import com.taskmgmt.service.AuthService;
import com.taskmgmt.service.AuthService.AuthResult;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        
        AuthResult result = authService.authenticate(request.getUsername(), request.getPassword());
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(new LoginResponse(result.getToken(), result.getUser()));
        } else {
            return ResponseEntity.status(401).body(new ErrorResponse(result.getError()));
        }
    }
    
    @Data
    static class LoginRequest {
        @NotBlank
        private String username;
        
        @NotBlank
        private String password;
    }
    
    @Data
    static class LoginResponse {
        private final String token;
        private final UserInfo user;
        
        LoginResponse(String token, com.taskmgmt.model.User user) {
            this.token = token;
            this.user = new UserInfo(user.getId(), user.getUsername(), user.getEmail(), user.getRole().toString());
        }
    }
    
    @Data
    static class UserInfo {
        private final Long id;
        private final String username;
        private final String email;
        private final String role;
    }
    
    @Data
    static class ErrorResponse {
        private final String error;
    }
}

