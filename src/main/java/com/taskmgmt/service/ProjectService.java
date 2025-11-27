package com.taskmgmt.service;

import com.taskmgmt.model.Project;
import com.taskmgmt.model.Project.ProjectStatus;
import com.taskmgmt.model.User;
import com.taskmgmt.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final TaskService taskService;
    private final CacheService cacheService;
    
    @Cacheable(value = "projectCache", key = "#id")
    public Project findById(Long id) {
        log.debug("Fetching project: {}", id);
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
    }
    
    public List<Project> findByUserId(Long userId) {
        return projectRepository.findByUserId(userId);
    }
    
    @Transactional
    @CacheEvict(value = {"projectCache", "projectStatsCache"}, allEntries = true)
    public Project createProject(String name, String description, User owner) {
        log.info("Creating project: {} by user: {}", name, owner.getUsername());
        
        Project project = Project.builder()
                .name(name)
                .description(description)
                .owner(owner)
                .status(ProjectStatus.ACTIVE)
                .build();
        
        // Owner is automatically a member
        project.getMembers().add(owner);
        
        Project savedProject = projectRepository.save(project);
        
        // Async cache update - might complete after transaction commits
        updateProjectStatsCache(savedProject.getId());
        
        return savedProject;
    }
    
    @Transactional
    @CacheEvict(value = {"projectCache", "projectStatsCache"}, key = "#projectId")
    public Project updateProject(Long projectId, String name, String description, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        
        // Authorization check - ambiguous: owner OR member?
        if (!project.getOwner().getId().equals(userId) && 
            !project.getMembers().stream().anyMatch(m -> m.getId().equals(userId))) {
            throw new SecurityException("User not authorized to update project");
        }
        
        if (name != null) {
            project.setName(name);
        }
        if (description != null) {
            project.setDescription(description);
        }
        
        project.setUpdatedAt(LocalDateTime.now());
        Project savedProject = projectRepository.save(project);
        
        // Invalidate cached stats
        updateProjectStatsCache(projectId);
        
        return savedProject;
    }
    
    @Transactional
    @CacheEvict(value = {"projectCache", "projectStatsCache"}, key = "#projectId")
    public void addMember(Long projectId, User member, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        
        // Only owner can add members - but check is inconsistent
        if (!project.getOwner().getId().equals(userId)) {
            throw new SecurityException("Only project owner can add members");
        }
        
        project.getMembers().add(member);
        projectRepository.save(project);
        
        // Cache invalidation
        updateProjectStatsCache(projectId);
    }
    
    @Transactional
    @CacheEvict(value = {"projectCache", "projectStatsCache"}, key = "#projectId")
    public void archiveProject(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        
        if (!project.getOwner().getId().equals(userId)) {
            throw new SecurityException("Only owner can archive project");
        }
        
        project.setStatus(ProjectStatus.ARCHIVED);
        project.setArchivedAt(LocalDateTime.now());
        projectRepository.save(project);
        
        // Cascade: Archive all tasks? Or leave them active?
        // Current implementation: tasks remain active - potential ambiguity
    }
    
    /**
     * Gets project statistics - uses cache that might be stale.
     */
    @Cacheable(value = "projectStatsCache", key = "#projectId")
    public String getProjectStats(Long projectId) {
        Project project = findById(projectId);
        
        // This query might be slow - no pagination
        List<com.taskmgmt.model.Task> tasks = taskService.findByProjectId(projectId);
        
        long todoCount = tasks.stream().filter(t -> t.getStatus() == com.taskmgmt.model.Task.TaskStatus.TODO).count();
        long inProgressCount = tasks.stream().filter(t -> t.getStatus() == com.taskmgmt.model.Task.TaskStatus.IN_PROGRESS).count();
        long completedCount = tasks.stream().filter(t -> t.getStatus() == com.taskmgmt.model.Task.TaskStatus.COMPLETED).count();
        
        String stats = String.format("Total: %d, TODO: %d, In Progress: %d, Completed: %d", 
                tasks.size(), todoCount, inProgressCount, completedCount);
        
        // Update cached field in project - might not be consistent
        project.setCachedStats(stats);
        projectRepository.save(project);
        
        return stats;
    }
    
    @Async
    public void updateProjectStatsCache(Long projectId) {
        // Async cache update - race condition possible
        try {
            cacheService.evictCache("projectStatsCache", projectId.toString());
        } catch (Exception e) {
            log.error("Failed to update cache for project: {}", projectId, e);
        }
    }
}

