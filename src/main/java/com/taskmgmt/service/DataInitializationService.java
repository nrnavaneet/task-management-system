package com.taskmgmt.service;

import com.taskmgmt.model.Project;
import com.taskmgmt.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to initialize sample data for development/testing.
 * Only runs in 'dev' profile.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService implements CommandLineRunner {
    private final UserService userService;
    private final ProjectService projectService;
    
    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing sample data...");
        
        try {
            // Create sample users
            User admin = userService.createUser(
                    "admin",
                    "admin@example.com",
                    "admin123",
                    "Admin User",
                    User.UserRole.ADMIN
            );
            
            User manager = userService.createUser(
                    "manager",
                    "manager@example.com",
                    "manager123",
                    "Manager User",
                    User.UserRole.MANAGER
            );
            
            // Create sample project
            Project project = projectService.createProject(
                    "Sample Project",
                    "A sample project for testing",
                    admin
            );
            
            log.info("Sample data initialization completed");
        } catch (Exception e) {
            log.error("Failed to initialize sample data", e);
        }
    }
}

