package com.taskmgmt.repository;

import com.taskmgmt.model.Project;
import com.taskmgmt.model.Project.ProjectStatus;
import com.taskmgmt.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwner(User owner);
    List<Project> findByStatus(ProjectStatus status);
    
    @Query("SELECT p FROM Project p JOIN p.members m WHERE m.id = :userId")
    List<Project> findByMemberId(@Param("userId") Long userId);
    
    @Query("SELECT p FROM Project p WHERE p.owner.id = :userId OR :userId IN (SELECT m.id FROM p.members m)")
    List<Project> findByUserId(@Param("userId") Long userId);
    
    Optional<Project> findByIdAndStatus(Long id, ProjectStatus status);
    
    // Potentially inefficient - no pagination
    @Query("SELECT p FROM Project p WHERE p.status = :status")
    List<Project> findAllByStatus(@Param("status") ProjectStatus status);
}

