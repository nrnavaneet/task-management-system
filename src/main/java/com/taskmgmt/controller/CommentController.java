package com.taskmgmt.controller;

import com.taskmgmt.model.Comment;
import com.taskmgmt.model.User;
import com.taskmgmt.service.CommentService;
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
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentController {
    private final CommentService commentService;
    
    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<Comment>> getCommentsByTask(@PathVariable Long taskId) {
        List<Comment> comments = commentService.findByTaskId(taskId);
        return ResponseEntity.ok(comments);
    }
    
    @PostMapping
    public ResponseEntity<Comment> createComment(
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal User user) {
        
        try {
            Comment comment = commentService.createComment(
                    request.getTaskId(),
                    request.getContent(),
                    user
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(comment);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Comment> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal User user) {
        
        try {
            Comment comment = commentService.updateComment(id, request.getContent(), user);
            return ResponseEntity.ok(comment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        
        try {
            commentService.deleteComment(id, user);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
    
    @Data
    static class CreateCommentRequest {
        @NotBlank
        private Long taskId;
        
        @NotBlank
        private String content;
    }
    
    @Data
    static class UpdateCommentRequest {
        @NotBlank
        private String content;
    }
}

