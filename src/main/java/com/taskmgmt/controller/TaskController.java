package com.taskmgmt.controller;

import com.taskmgmt.model.Task;
import com.taskmgmt.model.Task.TaskPriority;
import com.taskmgmt.model.Task.TaskStatus;
import com.taskmgmt.model.User;
import com.taskmgmt.service.TaskService;
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
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {
    private final TaskService taskService;
    
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Task>> getTasksByProject(@PathVariable Long projectId) {
        List<Task> tasks = taskService.findByProjectId(projectId);
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTask(@PathVariable Long id) {
        try {
            Task task = taskService.findById(id);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/my-tasks")
    public ResponseEntity<List<Task>> getMyTasks(@AuthenticationPrincipal User user) {
        List<Task> tasks = taskService.findByAssignee(user);
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/overdue")
    public ResponseEntity<List<Task>> getOverdueTasks(@AuthenticationPrincipal User user) {
        List<Task> tasks = taskService.findOverdueTasksForUser(user.getId());
        return ResponseEntity.ok(tasks);
    }
    
    @PostMapping
    public ResponseEntity<Task> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal User user) {
        
        try {
            TaskPriority priority = request.getPriority() != null ? 
                    TaskPriority.valueOf(request.getPriority()) : TaskPriority.MEDIUM;
            
            Task task = taskService.createTask(
                    request.getProjectId(),
                    request.getTitle(),
                    request.getDescription(),
                    priority,
                    user
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(task);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal User user) {
        
        try {
            TaskPriority priority = request.getPriority() != null ? 
                    TaskPriority.valueOf(request.getPriority()) : null;
            
            Task task = taskService.updateTask(
                    id,
                    request.getTitle(),
                    request.getDescription(),
                    priority,
                    user
            );
            
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
    
    @PostMapping("/{id}/assign")
    public ResponseEntity<Task> assignTask(
            @PathVariable Long id,
            @RequestBody AssignTaskRequest request,
            @AuthenticationPrincipal User user) {
        
        try {
            // Simplified - would need to fetch assignee user
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
    
    @PostMapping("/{id}/status")
    public ResponseEntity<Task> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal User user) {
        
        try {
            TaskStatus status = TaskStatus.valueOf(request.getStatus());
            Task task = taskService.updateTaskStatus(id, status, user);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
    
    @Data
    static class CreateTaskRequest {
        @NotBlank
        private Long projectId;
        
        @NotBlank
        private String title;
        private String description;
        private String priority;
    }
    
    @Data
    static class UpdateTaskRequest {
        private String title;
        private String description;
        private String priority;
    }
    
    @Data
    static class AssignTaskRequest {
        private Long assigneeId;
    }
    
    @Data
    static class UpdateStatusRequest {
        @NotBlank
        private String status;
    }
}

