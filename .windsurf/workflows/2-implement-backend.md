---
description: Backend
auto_execution_mode: 3
---

description: Implements backend logic based on an approved ADR
steps:
  - step: "Read the approved ADR."
  - step: "Generate the Domain Model (Aggregates, VOs) in strict pure Kotlin."
  - step: "Create the Use Cases and Ports."
  - step: "Implement the Infrastructure layer:
      1. Create JPA Entities and DTOs.
      2. Implement Repository Adapters.
      3. Create REST Controllers."
  - step: "Generate OpenAPI specs using the openapi-docs-generator."
  - step: "Write Spring Integration Tests for the happy path."