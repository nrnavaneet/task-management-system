package com.taskmgmt.repository;

import com.taskmgmt.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    
    // Legacy method - might return stale data
    Optional<User> findBySessionToken(String sessionToken);
    
    List<User> findByActiveTrue();
    List<User> findByRole(User.UserRole role);
    
    @Query("SELECT u FROM User u WHERE u.lastLoginAt > :since")
    List<User> findActiveUsersSince(@Param("since") LocalDateTime since);
    
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}

