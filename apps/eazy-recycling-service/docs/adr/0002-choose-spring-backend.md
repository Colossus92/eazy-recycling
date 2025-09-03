# ADR 0002: Choose Spring Boot with Kotlin for Backend Development

## Context and Problem Statement

We need a backend technology that can handle multiple external integrations with Dutch waste management systems and other parties. A key requirement is strong support for XML, since several integrations (e.g. EBA for digital waste transport contracts and LMA for waste collection registration) are XML-based. The chosen backend should also be flexible for adding future integrations and align with existing team knowledge.

## Considered Options
•	Spring Boot with Kotlin
•	Node.js

# Decision Outcome

Chosen option: Spring Boot with Kotlin.
The JVM ecosystem provides excellent support for XML processing and integration patterns. Spring Boot offers mature libraries, stability, and wide adoption in enterprise integrations. Kotlin adds modern language features with strong interoperability with Java. This makes it the best fit for handling the XML-heavy requirements while still allowing for rapid development and maintainability.

##Pros and Cons of the Options

### Spring Boot with Kotlin

Pros
•	Excellent XML support (JAXB, Jackson XML, etc.).
•	Mature ecosystem for enterprise integrations.
•	Strong support for structured APIs, security, and messaging.
•	Aligns with existing knowledge and experience.
•	Kotlin provides concise, modern syntax while leveraging JVM libraries.

Cons
•	Smaller hiring pool for Kotlin developers compared to Node.js.
•	Higher resource usage compared to lightweight Node.js apps.

⸻

### Node.js

Pros
•	Very large ecosystem and hiring pool.
•	Easy to build lightweight services quickly.
•	Good support for JSON and REST APIs.

Cons
•	Limited XML handling compared to JVM ecosystem.
•	Less mature tooling for enterprise integration patterns.
•	Would require more workarounds for XML-heavy use cases.