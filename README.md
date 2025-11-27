# Task Management System

A multi-layered task management system built with Spring Boot.

## Overview

This system manages projects, tasks, users, and comments with authentication and authorization.

## Architecture

- **Controllers**: REST API endpoints
- **Services**: Business logic layer
- **Repositories**: Data access layer
- **Models**: Domain entities

## Getting Started

```bash
mvn spring-boot:run
```

The application will start on port 8080.

## API Endpoints

- `/api/auth/login` - User authentication
- `/api/projects` - Project management
- `/api/tasks` - Task management
- `/api/users` - User management

## Database

H2 in-memory database is used for development. Access console at `/h2-console`.

## Note

This codebase contains some legacy code patterns and intentional complexity for educational purposes.

