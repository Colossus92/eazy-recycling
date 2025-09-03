# ADR 0003: Prefer Third-Party Tools Over Custom Development

##Context and Problem Statement

We need to decide whether to build certain foundational or supporting components ourselves, or rely on existing third-party tools. Building everything in-house provides maximum control but also requires significant time investment. At this stage, time-to-market and focus on core functionality are more important than minimizing monthly operating costs.

Considered Options
•	Build components in-house.
•	Use third-party tools where possible.

## Decision Outcome

Chosen option: Use third-party tools where possible.
Although relying on external providers introduces ongoing monthly costs, the trade-off is acceptable. Using established third-party services allows us to save valuable development time, move faster, and focus on building the unique parts of our product. Custom solutions may be considered later when the scale, costs, or specific requirements justify the effort.

## Pros and Cons of the Options

### Build components in-house

Pros
•	Full control over implementation and roadmap.
•	No external vendor dependency.
•	Potential long-term cost savings if usage grows.

Cons
•	High upfront development effort.
•	Slower time-to-market.
•	Maintenance burden and reduced focus on core features.

⸻

### Use third-party tools

Pros
•	Rapid adoption, minimal setup time.
•	Access to mature, battle-tested solutions.
•	Enables focus on core product development.
•	Reduces maintenance overhead.

Cons
•	Recurring monthly costs.
•	Dependency on external providers and their reliability.
•	Risk of vendor lock-in.