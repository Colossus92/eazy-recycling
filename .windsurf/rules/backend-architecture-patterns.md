---
trigger: always_on
---

---
description: Standards for Spring Boot, Kotlin, and DDD implementation
globs: "**/*.kt"
---
# Backend Architecture Standards

## Core Architecture
- **Pattern**: DDD with Hexagonal Architecture (Ports & Adapters).
- **Structure**:
  - `domain`: Pure Kotlin. Contains Aggregates, Entities, Value Objects. No frameworks.
  - `application`: Use Cases / Services orchestrating domain logic.
  - `infrastructure`: Adapters (Rest Controllers, JPA Repositories).
  - `ports`: Interfaces defined in `domain` or `application`, implemented in `infrastructure`.

## Implementation Rules
- **Entities**: Mutable, have identity.
- **Value Objects**: Immutable data classes.
- **Repositories**: 
  - Define interface in `domain/ports`.
  - Implement JpaRepository in `infrastructure`.
  - Use DTOs for DB persistence; map to/from Domain Entities.
- **Testing**: 
  - Domain logic: JUnit 5 (Unit tests).
  - Infrastructure/Flows: Spring Integration Tests (`@SpringBootTest`).