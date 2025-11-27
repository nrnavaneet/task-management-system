package com.taskmgmt.controller;

import com.taskmgmt.model.Project;
import com.taskmgmt.model.User;
import com.taskmgmt.service.ProjectService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {
    private final ProjectService projectService;
    
    @GetMapping
    public ResponseEntity<List<Project>> getProjects(@AuthenticationPrincipal User user) {
        List<Project> projects = projectService.findByUserId(user.getId());
        return ResponseEntity.ok(projects);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable Long id) {
        try {
            Project project = projectService.findById(id);
            return ResponseEntity.ok(project);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/{id}/stats")
    public ResponseEntity<String> getProjectStats(@PathVariable Long id) {
        try {
            String stats = projectService.getProjectStats(id);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping
    public ResponseEntity<Project> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal User user) {
        
        try {
            Project project = projectService.createProject(
                    request.getName(),
                    request.getDescription(),
                    user
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(project);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal User user) {
        
        try {
            Project project = projectService.updateProject(
                    id,
                    request.getName(),
                    request.getDescription(),
                    user.getId()
            );
            return ResponseEntity.ok(project);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
    
    @PostMapping("/{id}/members")
    public ResponseEntity<Void> addMember(
            @PathVariable Long id,
            @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal User user) {
        
        try {
            // This would need UserService to fetch the member
            // Simplified for now - actual implementation would fetch user
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
    
    @PostMapping("/{id}/archive")
    public ResponseEntity<Void> archiveProject(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        
        try {
            projectService.archiveProject(id, user.getId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
    
    @Data
    static class CreateProjectRequest {
        @NotBlank
        private String name;
        private String description;
    }
    
    @Data
    static class UpdateProjectRequest {
        private String name;
        private String description;
    }
    
    @Data
    static class AddMemberRequest {
        private Long userId;
    }
}

