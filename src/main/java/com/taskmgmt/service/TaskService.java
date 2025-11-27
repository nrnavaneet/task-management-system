package com.taskmgmt.service;

import com.taskmgmt.model.Project;
import com.taskmgmt.model.Task;
import com.taskmgmt.model.Task.TaskStatus;
import com.taskmgmt.model.User;
import com.taskmgmt.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final NotificationService notificationService;
    
    public Task findById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
    }
    
    public List<Task> findByProjectId(Long projectId) {
        // Potential N+1 problem: comments are lazy-loaded
        return taskRepository.findByProjectId(projectId);
    }
    
    public List<Task> findByAssignee(User assignee) {
        return taskRepository.findByAssignee(assignee);
    }
    
    @Transactional
    @CacheEvict(value = {"projectCache", "projectStatsCache"}, key = "#projectId")
    public Task createTask(Long projectId, String title, String description, 
                          Task.TaskPriority priority, User creator) {
        log.info("Creating task: {} in project: {}", title, projectId);
        
        Project project = projectService.findById(projectId);
        
        // Authorization: creator must be owner or member
        boolean isAuthorized = project.getOwner().getId().equals(creator.getId()) ||
                project.getMembers().stream().anyMatch(m -> m.getId().equals(creator.getId()));
        
        if (!isAuthorized) {
            throw new SecurityException("User not authorized to create tasks in this project");
        }
        
        Task task = Task.builder()
                .title(title)
                .description(description)
                .priority(priority != null ? priority : Task.TaskPriority.MEDIUM)
                .status(TaskStatus.TODO)
                .project(project)
                .build();
        
        // Add to status history
        Task.StatusHistoryEntry entry = new Task.StatusHistoryEntry(
                TaskStatus.TODO, 
                LocalDateTime.now(), 
                creator.getUsername()
        );
        task.getStatusHistory().add(entry);
        
        Task savedTask = taskRepository.save(task);
        
        // Side effect: Notify project members
        try {
            notificationService.notifyTaskCreated(savedTask, creator);
        } catch (Exception e) {
            log.error("Failed to send task creation notification", e);
        }
        
        return savedTask;
    }
    
    @Transactional
    @CacheEvict(value = {"projectCache", "projectStatsCache"}, allEntries = true)
    public Task updateTask(Long taskId, String title, String description, 
                          Task.TaskPriority priority, User updater) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        
        // Authorization check - ambiguous rules
        boolean isAuthorized = checkTaskUpdatePermission(task, updater);
        if (!isAuthorized) {
            throw new SecurityException("User not authorized to update task");
        }
        
        if (title != null) {
            task.setTitle(title);
        }
        if (description != null) {
            task.setDescription(description);
        }
        if (priority != null) {
            task.setPriority(priority);
        }
        
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }
    
    @Transactional
    @CacheEvict(value = {"projectCache", "projectStatsCache"}, allEntries = true)
    public Task assignTask(Long taskId, User assignee, User assigner) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        
        // Authorization: assigner must be project owner or manager
        Project project = task.getProject();
        boolean canAssign = project.getOwner().getId().equals(assigner.getId()) ||
                assigner.getRole() == User.UserRole.MANAGER ||
                assigner.getRole() == User.UserRole.ADMIN;
        
        if (!canAssign) {
            throw new SecurityException("User not authorized to assign tasks");
        }
        
        task.setAssignee(assignee);
        task.setUpdatedAt(LocalDateTime.now());
        Task savedTask = taskRepository.save(task);
        
        // Side effect: Notify assignee
        try {
            notificationService.notifyTaskAssigned(savedTask, assignee);
        } catch (Exception e) {
            log.error("Failed to send assignment notification", e);
        }
        
        return savedTask;
    }
    
    @Transactional
    @CacheEvict(value = {"projectCache", "projectStatsCache"}, allEntries = true)
    public Task updateTaskStatus(Long taskId, TaskStatus newStatus, User updater) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        
        // Optimistic locking check - version field might not be used everywhere
        TaskStatus oldStatus = task.getStatus();
        
        boolean isAuthorized = checkTaskUpdatePermission(task, updater);
        if (!isAuthorized) {
            throw new SecurityException("User not authorized to change task status");
        }
        
        task.setStatus(newStatus);
        task.setUpdatedAt(LocalDateTime.now());
        
        // Add to status history - might have duplicates if concurrent updates
        Task.StatusHistoryEntry entry = new Task.StatusHistoryEntry(
                newStatus, 
                LocalDateTime.now(), 
                updater.getUsername()
        );
        task.getStatusHistory().add(entry);
        
        if (newStatus == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        }
        
        Task savedTask = taskRepository.save(task);
        
        // Side effect: Notify on status change
        if (oldStatus != newStatus) {
            try {
                notificationService.notifyTaskStatusChanged(savedTask, oldStatus, newStatus);
            } catch (Exception e) {
                log.error("Failed to send status change notification", e);
            }
        }
        
        return savedTask;
    }
    
    /**
     * Authorization logic - complex and potentially ambiguous
     */
    private boolean checkTaskUpdatePermission(Task task, User user) {
        Project project = task.getProject();
        
        // Owner can always update
        if (project.getOwner().getId().equals(user.getId())) {
            return true;
        }
        
        // Admin can always update
        if (user.getRole() == User.UserRole.ADMIN) {
            return true;
        }
        
        // Assignee can update their own tasks
        if (task.getAssignee() != null && task.getAssignee().getId().equals(user.getId())) {
            return true;
        }
        
        // Managers in the project can update
        if (user.getRole() == User.UserRole.MANAGER && 
            project.getMembers().stream().anyMatch(m -> m.getId().equals(user.getId()))) {
            return true;
        }
        
        return false;
    }
    
    public List<Task> findOverdueTasksForUser(Long userId) {
        return taskRepository.findOverdueTasksForUser(userId, LocalDateTime.now());
    }
}

