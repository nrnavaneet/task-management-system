package com.taskmgmt.service;

import com.taskmgmt.model.Comment;
import com.taskmgmt.model.Task;
import com.taskmgmt.model.User;
import com.taskmgmt.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    private final CommentRepository commentRepository;
    private final TaskService taskService;
    
    public List<Comment> findByTaskId(Long taskId) {
        // Uses soft delete filter
        return commentRepository.findActiveCommentsByTaskId(taskId);
    }
    
    @Transactional
    public Comment createComment(Long taskId, String content, User author) {
        log.info("Creating comment on task: {} by user: {}", taskId, author.getUsername());
        
        Task task = taskService.findById(taskId);
        
        // Authorization: author must be project member
        boolean isAuthorized = task.getProject().getOwner().getId().equals(author.getId()) ||
                task.getProject().getMembers().stream().anyMatch(m -> m.getId().equals(author.getId()));
        
        if (!isAuthorized) {
            throw new SecurityException("User not authorized to comment on this task");
        }
        
        Comment comment = Comment.builder()
                .content(content)
                .task(task)
                .author(author)
                .deleted(false)
                .build();
        
        return commentRepository.save(comment);
    }
    
    @Transactional
    public Comment updateComment(Long commentId, String content, User updater) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        
        if (comment.getDeleted()) {
            throw new IllegalArgumentException("Cannot update deleted comment");
        }
        
        // Authorization: only author can update
        if (!comment.getAuthor().getId().equals(updater.getId())) {
            throw new SecurityException("Only comment author can update");
        }
        
        comment.setContent(content);
        return commentRepository.save(comment);
    }
    
    @Transactional
    public void deleteComment(Long commentId, User deleter) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        
        // Authorization: author or project owner/admin can delete
        boolean canDelete = comment.getAuthor().getId().equals(deleter.getId()) ||
                comment.getTask().getProject().getOwner().getId().equals(deleter.getId()) ||
                deleter.getRole() == User.UserRole.ADMIN;
        
        if (!canDelete) {
            throw new SecurityException("User not authorized to delete comment");
        }
        
        // Soft delete - comment remains in database
        comment.setDeleted(true);
        commentRepository.save(comment);
    }
}

