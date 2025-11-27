package com.taskmgmt.repository;

import com.taskmgmt.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTaskId(Long taskId);
    List<Comment> findByAuthorId(Long authorId);
    
    @Query("SELECT c FROM Comment c WHERE c.task.id = :taskId AND c.deleted = false")
    List<Comment> findActiveCommentsByTaskId(@Param("taskId") Long taskId);
    
    // Soft delete query - might miss some edge cases
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.task.id = :taskId AND c.deleted = false")
    long countActiveCommentsByTaskId(@Param("taskId") Long taskId);
}

