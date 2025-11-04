---
trigger: always_on
---

In the backend (the springboot-backend folder, with Spring and Kotlin code). Apply Domain-Driven-Design and hexagonal architecture best practices.

Till date there are some simple entities (like processing-method, eural code, truck, wastecontainers) which do not necessarily need to be in this architecture style, because of their simplicity. But do apply patterns for the core aggregates, ContainerTransport, WasteTransport, WasteStream, Company.