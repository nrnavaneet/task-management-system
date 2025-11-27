package com.taskmgmt.repository;

import com.taskmgmt.model.Task;
import com.taskmgmt.model.Task.TaskStatus;
import com.taskmgmt.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectId(Long projectId);
    List<Task> findByAssignee(User assignee);
    List<Task> findByStatus(TaskStatus status);
    
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.status = :status")
    List<Task> findByProjectIdAndStatus(@Param("projectId") Long projectId, @Param("status") TaskStatus status);
    
    @Query("SELECT t FROM Task t WHERE t.assignee.id = :userId AND t.dueDate < :date AND t.status != 'COMPLETED'")
    List<Task> findOverdueTasksForUser(@Param("userId") Long userId, @Param("date") LocalDateTime date);
    
    // N+1 potential - tasks loaded without eager fetching of comments
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId")
    List<Task> findAllByProjectId(@Param("projectId") Long projectId);
    
    long countByProjectIdAndStatus(Long projectId, TaskStatus status);
}

