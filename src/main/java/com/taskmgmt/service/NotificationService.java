package com.taskmgmt.service;

import com.taskmgmt.model.Task;
import com.taskmgmt.model.Task.TaskStatus;
import com.taskmgmt.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Notification service - sends emails/notifications.
 * Currently a stub implementation - might fail silently.
 */
@Service
@Slf4j
public class NotificationService {
    
    @Async
    public void sendWelcomeEmail(User user) {
        // TODO: Implement actual email sending
        log.info("Sending welcome email to: {}", user.getEmail());
        // Simulate async operation
    }
    
    @Async
    public void sendDeactivationEmail(User user) {
        log.info("Sending deactivation email to: {}", user.getEmail());
    }
    
    @Async
    public void notifyTaskCreated(Task task, User creator) {
        log.info("Notifying project members about new task: {}", task.getTitle());
        // Would iterate through project members and send notifications
        // Currently no-op
    }
    
    @Async
    public void notifyTaskAssigned(Task task, User assignee) {
        log.info("Notifying assignee: {} about task: {}", assignee.getEmail(), task.getTitle());
    }
    
    @Async
    public void notifyTaskStatusChanged(Task task, TaskStatus oldStatus, TaskStatus newStatus) {
        log.info("Notifying about task status change: {} -> {}", oldStatus, newStatus);
        // Would notify project members and assignee
    }
}

