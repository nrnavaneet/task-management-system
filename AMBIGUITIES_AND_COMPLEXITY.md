# Ambiguities and Complexities in Task Management System

This document outlines the intentional ambiguities and complexities in the codebase that make it suitable for deep reasoning evaluation.

## 1. Dual Authentication Systems

**Location**: `AuthService.java`, `LegacyAuthService.java`, `JwtTokenService.java`

**Ambiguity**: The codebase maintains two authentication systems simultaneously:
- **Legacy System**: Uses session tokens stored in the database (`User.sessionToken`)
- **JWT System**: Stateless JWT tokens

**Complexity Points**:
- `AuthService.authenticate()` chooses between systems based on `legacyAuthEnabled` flag and whether user has `sessionToken` set
- `AuthService.validateToken()` tries legacy first, then JWT
- The decision logic is not immediately obvious - requires tracing through multiple services
- Legacy system updates `lastLoginAt` on token validation (side effect)
- Configuration flag `legacy.auth.enabled` in `application.properties`

**Questions this enables**:
- "How does authentication work when a user logs in? What determines which system is used?"
- "What happens when legacy auth is enabled but a user doesn't have a sessionToken?"
- "How does token validation differ between legacy and JWT tokens?"

## 2. Authorization Rules - Multiple Overlapping Patterns

**Location**: `TaskService.java`, `ProjectService.java`, `CommentService.java`

**Ambiguity**: Authorization checks are inconsistent across different operations:
- Task updates: Owner, Admin, Assignee, or Manager (in project)
- Project updates: Owner OR member (ambiguous)
- Comment updates: Only author
- Comment deletion: Author OR project owner OR admin

**Complexity Points**:
- `TaskService.checkTaskUpdatePermission()` has complex logic with multiple conditions
- `ProjectService.updateProject()` checks owner OR member, but addMember() only checks owner
- No centralized authorization service - logic scattered
- Different methods check different combinations of roles

**Questions this enables**:
- "Who can update a task? Trace through all the authorization checks."
- "What's the difference between updating a project and adding a member to it in terms of authorization?"
- "If a user is both the assignee and a manager, what permissions do they have?"

## 3. State Management and Side Effects

**Location**: Multiple service classes

**Ambiguity**: State changes have cascading side effects:
- Task status changes update `completedAt`, add to `statusHistory`, trigger notifications
- User deactivation clears `sessionToken`
- Project creation automatically adds owner as member
- Status history might have duplicates if concurrent updates occur

**Complexity Points**:
- `Task.preUpdate()` sets `completedAt` when status becomes COMPLETED
- Status history is added in `TaskService.updateTaskStatus()` but not in all update paths
- Notifications are sent asynchronously and don't roll back on failure
- Cache invalidation happens asynchronously - potential race conditions

**Questions this enables**:
- "What side effects occur when a task status is changed to COMPLETED?"
- "How is task status history maintained? Can it become inconsistent?"
- "What happens if a notification fails to send during task creation?"

## 4. Caching Strategy - Potential Staleness

**Location**: `ProjectService.java`, `CacheConfig.java`, `UserService.java`

**Ambiguity**: Cache invalidation is inconsistent:
- Project stats cache is evicted on many operations but updated asynchronously
- `Project.cachedStats` field might be stale
- User cache is evicted on updates but might not be consistent across nodes
- Async cache updates might complete after transaction commits

**Complexity Points**:
- `ProjectService.updateProjectStatsCache()` is async - race conditions possible
- `ProjectService.getProjectStats()` updates the cached field but cache might not be refreshed
- Cache keys and eviction strategies are not obvious
- No clear cache warming strategy

**Questions this enables**:
- "How does the project statistics cache get updated? When might it be stale?"
- "What happens if a project is updated while the cache refresh is in progress?"
- "How is cache consistency maintained when multiple users update the same project?"

## 5. Transaction Boundaries and Rollback Behavior

**Location**: Service classes with `@Transactional`

**Ambiguity**: Not all operations are transactional, and side effects don't always rollback:
- User creation sends welcome email - failure doesn't rollback user creation
- Task creation sends notifications - async, no rollback
- Cache invalidation happens outside transactions
- Status history updates might not be atomic

**Complexity Points**:
- NotificationService methods are async - failures are logged but don't affect main flow
- `UserService.createUser()` has try-catch around notification that swallows exceptions
- Some operations that should be transactional might not be (e.g., cache updates)
- Version field in Task is for optimistic locking but might not be used consistently

**Questions this enables**:
- "What happens if the welcome email fails to send during user creation?"
- "Are all operations in task creation atomic? What can fail without rolling back?"
- "How does optimistic locking work for task updates? What happens on concurrent modifications?"

## 6. Data Access Patterns - N+1 Potential

**Location**: Repository queries, service methods

**Ambiguity**: Lazy loading and query patterns can cause performance issues:
- `TaskRepository.findAllByProjectId()` doesn't eagerly fetch comments
- Project members are lazy-loaded - multiple queries when iterating
- Task status history is an ElementCollection - loaded separately
- No pagination on some queries (e.g., `ProjectRepository.findAllByStatus()`)

**Complexity Points**:
- Comments on tasks are lazy-loaded by default
- Project members relationship is `@ManyToMany` with lazy fetch
- Queries like `findByProjectIdAndStatus()` don't specify fetch strategy
- Potential N+1 when loading tasks with their comments

**Questions this enables**:
- "Where are N+1 query problems likely to occur in this codebase?"
- "How are task comments loaded when fetching tasks for a project?"
- "What queries would be executed when loading a project with all its tasks and members?"

## 7. Error Handling Propagation

**Location**: Controllers, GlobalExceptionHandler, Services

**Ambiguity**: Error handling is inconsistent:
- Some services throw `IllegalArgumentException`, others throw `SecurityException`
- GlobalExceptionHandler catches all but some errors might be swallowed
- Async operations log errors but don't propagate
- Validation happens at controller level (Bean Validation) and service level (manual checks)

**Complexity Points**:
- `GlobalExceptionHandler` converts exceptions to HTTP responses
- Some controllers catch exceptions and convert manually
- Async notification failures are logged but don't affect response
- Security exceptions are caught in some places but not others

**Questions this enables**:
- "How does error handling propagate from service layer to the client?"
- "What happens if an async notification fails? Does the user know?"
- "Where are validation errors caught and how are they formatted for the API response?"

## 8. Legacy Code Patterns

**Location**: `LegacyAuthService.java`, `User.java`, `application.properties`

**Ambiguity**: Legacy code coexists with new code:
- `User.sessionToken` field has TODO comment to remove after migration
- `LegacyAuthService` has deprecation notes
- Configuration flag controls legacy system
- Migration timeline mentioned (Q2 2024) but not enforced

**Complexity Points**:
- Legacy auth updates `lastLoginAt` on token validation (side effect)
- New JWT system doesn't update `lastLoginAt` in the same way
- Both systems need to be maintained
- No clear migration path documented

**Questions this enables**:
- "What's the difference between the legacy and new authentication systems?"
- "How would you migrate from legacy auth to JWT-only?"
- "What code would need to change if legacy auth is completely removed?"

## 9. Soft Delete Pattern

**Location**: `Comment.java`, `CommentService.java`, `CommentRepository.java`

**Ambiguity**: Comments use soft delete but other entities don't:
- Comments have `deleted` Boolean field
- Soft delete queries filter by `deleted = false`
- Other entities (Tasks, Projects) don't use soft delete
- Inconsistent pattern across the codebase

**Complexity Points**:
- `CommentRepository.findActiveCommentsByTaskId()` filters by `deleted = false`
- But `findByTaskId()` doesn't - might return deleted comments
- Soft delete is not used consistently - Comments yes, Tasks/Projects no
- No audit trail for when comments were deleted

**Questions this enables**:
- "How are deleted comments handled differently from other deleted entities?"
- "What queries return deleted comments and which ones don't?"
- "Why do comments use soft delete but tasks and projects don't?"

## 10. Status History Tracking

**Location**: `Task.java`, `TaskService.java`

**Ambiguity**: Status history tracking has potential issues:
- Status history is added in `updateTaskStatus()` but not in constructor
- Concurrent updates might create duplicate history entries
- History is stored as `ElementCollection` - separate table
- No mechanism to prevent duplicate entries

**Complexity Points**:
- Status history entry is created manually in service
- Initial status from constructor isn't added to history
- No uniqueness constraint on status history entries
- Version field exists for optimistic locking but might not prevent history duplicates

**Questions this enables**:
- "How is task status history maintained? Can it have duplicate entries?"
- "What happens to status history when multiple users update a task concurrently?"
- "Why doesn't the initial task status appear in the status history?"

## Sample Questions for Evaluation

Based on these ambiguities, here are example questions that require deep reasoning:

1. **Execution Flow**: "How does a user request to create a task get processed from the HTTP endpoint to database storage, including all side effects?"

2. **Architecture**: "What design patterns are used for authentication in this codebase? How do the legacy and JWT systems coexist?"

3. **State Management**: "What are all the places where a User model gets updated? Include direct updates and side effects."

4. **Error Handling**: "What happens if the notification service fails when creating a task? How does the error propagate?"

5. **Dependencies**: "If you wanted to remove the legacy authentication system, what files would need changes and what are the dependencies?"

6. **Performance**: "Where are potential N+1 query problems in the task loading functionality?"

7. **Security**: "How is authorization enforced for updating tasks? Trace through all the checks."

8. **Legacy Code**: "What's the difference between legacy and JWT authentication? How would you migrate from one to the other?"

9. **Data Models**: "How is task status history stored and what are the relationships?"

10. **Technical Debt**: "What TODO comments exist and what technical debt do they represent?"

