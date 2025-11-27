# Task Management System - Project Summary

## Overview

This is a Java-based task management system built with Spring Boot. It's designed to be a realistic codebase with intentional complexities and ambiguities suitable for evaluating AI model reasoning capabilities.

## Project Statistics

- **Language**: Java 11
- **Framework**: Spring Boot 2.7.0
- **Files**: 34 Java files, 38 total code files
- **Lines of Code**: ~2,500 lines
- **Size Category**: Small (3-50 files, 200-10,000 lines)

## Architecture

### Multi-Layered Structure

```
Controllers (REST API)
    ↓
Services (Business Logic)
    ↓
Repositories (Data Access)
    ↓
Models (Domain Entities)
```

### Key Components

1. **Models** (`model/`):
   - `User` - User accounts with roles and authentication
   - `Project` - Project management with members
   - `Task` - Tasks with status, priority, and assignees
   - `Comment` - Comments on tasks with soft delete

2. **Repositories** (`repository/`):
   - JPA repositories for data access
   - Custom queries for complex operations
   - Potential N+1 query patterns

3. **Services** (`service/`):
   - `AuthService` - Dual authentication (JWT + Legacy)
   - `UserService` - User management
   - `ProjectService` - Project operations with caching
   - `TaskService` - Task management with authorization
   - `CommentService` - Comment operations
   - `NotificationService` - Async notifications
   - `BackgroundJobService` - Scheduled jobs

4. **Controllers** (`controller/`):
   - RESTful API endpoints
   - Request/response handling
   - Authentication integration

5. **Security** (`security/`, `config/`):
   - JWT authentication filter
   - Legacy token support
   - Security configuration
   - User details service

6. **Cross-Cutting Concerns**:
   - Global exception handling
   - Caching configuration
   - Async processing
   - CORS configuration
   - Validation utilities

## Key Features

### 1. Dual Authentication System
- Legacy session-based authentication (stateful)
- Modern JWT-based authentication (stateless)
- Both systems coexist with feature flag control

### 2. Complex Authorization
- Role-based access control (Admin, Manager, Developer, Viewer)
- Resource-level permissions (owner, member, assignee)
- Multiple overlapping authorization patterns

### 3. State Management
- Status tracking with history
- Soft deletes for comments
- Cache invalidation strategies
- Async side effects

### 4. Error Handling
- Global exception handler
- Service-level error handling
- Async operation error handling

### 5. Performance Considerations
- Caching (Caffeine-based)
- Lazy loading relationships
- Potential N+1 query patterns
- Async processing for notifications

## Intentional Complexities

See `AMBIGUITIES_AND_COMPLEXITY.md` for detailed analysis of:
- Dual authentication systems
- Inconsistent authorization rules
- Side effects and state management
- Caching inconsistencies
- Transaction boundaries
- Legacy code patterns
- Soft delete inconsistencies

## Technology Stack

- **Spring Boot** 2.7.0
- **Spring Data JPA** - Database access
- **Spring Security** - Authentication/authorization
- **H2 Database** - In-memory database
- **Lombok** - Code generation
- **JWT** (jjwt) - Token generation
- **Caffeine** - Caching
- **Bean Validation** - Input validation

## Getting Started

### Prerequisites
- Java 11 or higher
- Maven 3.6+

### Running the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### H2 Console
Access the database console at: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:taskdb`
- Username: `sa`
- Password: (empty)

### API Endpoints

- `POST /api/auth/login` - User login
- `GET /api/users` - List users
- `GET /api/projects` - List projects
- `GET /api/tasks/project/{id}` - List tasks for project
- `POST /api/tasks` - Create task
- `GET /api/comments/task/{id}` - Get comments for task

## Codebase Characteristics

### Real-World Patterns
- Multi-layered architecture
- Dependency injection
- Repository pattern
- Service layer abstraction
- DTO pattern (partial)

### Realistic Imperfections
- Inconsistent authorization checks
- Mixed authentication systems
- Some N+1 query potential
- Cache invalidation races
- TODO comments for technical debt
- Legacy code patterns
- Inconsistent error handling

### Cross-Cutting Concerns
- Logging throughout
- Security annotations
- Transaction management
- Caching annotations
- Async processing
- Global exception handling

## Question Categories Supported

This codebase supports questions from all 10 categories:

1. **Execution Flow Tracing** - Complex request flows through multiple layers
2. **Architecture & Design Patterns** - Dependency injection, repository pattern, dual auth
3. **State Management & Side Effects** - Status history, cache updates, notifications
4. **Error Handling & Edge Cases** - Global handler, async failures, validation
5. **Dependencies & Coupling** - Service dependencies, auth systems
6. **Performance & Optimization** - Caching, N+1 queries, lazy loading
7. **Security & Authorization** - Multiple auth systems, complex permissions
8. **Testing & Observability** - Logging infrastructure, error tracking
9. **Data Models & Schema** - Entity relationships, status history, soft deletes
10. **Legacy Code & Technical Debt** - Legacy auth, TODO comments, migration paths

## File Structure

```
task-management-system/
├── pom.xml
├── README.md
├── AMBIGUITIES_AND_COMPLEXITY.md
├── PROJECT_SUMMARY.md
├── .gitignore
└── src/
    └── main/
        ├── java/com/taskmgmt/
        │   ├── TaskManagementApplication.java
        │   ├── model/
        │   │   ├── User.java
        │   │   ├── Project.java
        │   │   ├── Task.java
        │   │   └── Comment.java
        │   ├── repository/
        │   │   ├── UserRepository.java
        │   │   ├── ProjectRepository.java
        │   │   ├── TaskRepository.java
        │   │   └── CommentRepository.java
        │   ├── service/
        │   │   ├── AuthService.java
        │   │   ├── LegacyAuthService.java
        │   │   ├── JwtTokenService.java
        │   │   ├── UserService.java
        │   │   ├── ProjectService.java
        │   │   ├── TaskService.java
        │   │   ├── CommentService.java
        │   │   ├── NotificationService.java
        │   │   ├── CacheService.java
        │   │   ├── BackgroundJobService.java
        │   │   └── DataInitializationService.java
        │   ├── controller/
        │   │   ├── AuthController.java
        │   │   ├── UserController.java
        │   │   ├── ProjectController.java
        │   │   ├── TaskController.java
        │   │   └── CommentController.java
        │   ├── security/
        │   │   ├── JwtAuthenticationFilter.java
        │   │   └── CustomUserDetailsService.java
        │   ├── config/
        │   │   ├── SecurityConfig.java
        │   │   ├── CacheConfig.java
        │   │   ├── AsyncConfig.java
        │   │   └── WebConfig.java
        │   ├── exception/
        │   │   └── GlobalExceptionHandler.java
        │   ├── dto/
        │   │   └── TaskDTO.java
        │   └── util/
        │       └── ValidationUtil.java
        └── resources/
            └── application.properties
```

## Notes for Evaluators

- This codebase intentionally contains ambiguities and complexities
- Authorization rules are inconsistent by design
- Dual authentication systems coexist to demonstrate legacy patterns
- Some operations have side effects that may not be obvious
- Cache invalidation has potential race conditions
- Error handling patterns vary across the codebase
- See AMBIGUITIES_AND_COMPLEXITY.md for detailed analysis

## License

This is a synthetic codebase created for educational and evaluation purposes.

