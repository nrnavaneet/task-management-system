package com.taskmgmt.dto;

import com.taskmgmt.model.Task;
import lombok.Data;

/**
 * Data Transfer Object for Task.
 * Used for API responses to avoid exposing internal model structure.
 */
@Data
public class TaskDTO {
    private Long id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private Long projectId;
    private String projectName;
    private Long assigneeId;
    private String assigneeName;
    
    public static TaskDTO fromEntity(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus().toString());
        dto.setPriority(task.getPriority().toString());
        dto.setProjectId(task.getProject().getId());
        dto.setProjectName(task.getProject().getName());
        
        if (task.getAssignee() != null) {
            dto.setAssigneeId(task.getAssignee().getId());
            dto.setAssigneeName(task.getAssignee().getFullName());
        }
        
        return dto;
    }
}

