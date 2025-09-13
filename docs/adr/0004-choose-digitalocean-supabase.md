# ADR 0004: Use DigitalOcean App Platform and Supabase as Core Infrastructure

## Context and Problem Statement

We need a reliable, cost-effective, and developer-friendly infrastructure setup to host our applications and manage backend services. While enterprise cloud providers offer extensive services, they can be complex to configure correctly and often come with steep learning curves. At this stage, simplicity and speed of iteration are more important than access to every advanced feature.

## Considered Options
* DigitalOcean App Platform + Supabase
* AWS
* Google Firebase

## Decision Outcome

Chosen option: DigitalOcean App Platform combined with Supabase.
These two services complement each other and together provide most of the functionality that would otherwise require a more complex AWS setup.
* Supabase offers S3-compatible storage, functions, authentication, and a Postgres database. Its configuration can be stored in version control, which fits well with our workflow. It is cost-effective and allows us to move quickly without sacrificing flexibility.
* DigitalOcean App Platform provides a simple way to deploy and monitor applications, with minimal setup overhead. It reduces operational complexity while still covering core deployment needs.

Together, they enable a lean, manageable setup that aligns with our priorities at this stage.

## Pros and Cons of the Options

### DigitalOcean App Platform + Supabase

Pros
* Cost-effective compared to enterprise cloud setups.
* Supabase offers a powerful backend (Postgres, storage, auth, functions) with minimal setup.
* Configuration can be version controlled.
* DigitalOcean App Platform makes deployments and monitoring simple and accessible.
* Combination covers most services typically used in AWS or similar providers.
* Allows focus on product rather than infrastructure management.

Cons
* Less flexibility and fewer advanced services compared to AWS.
* Reliance on two separate vendors.
* Potential need to migrate to larger cloud providers in the future as requirements scale.

⸻

### AWS

Pros
* Full-featured, enterprise-grade services.
* High flexibility and scalability.
* Large ecosystem and strong community.

Cons
* Steeper learning curve to set up correctly.
* Higher operational overhead.
* Can become costly without careful optimization.

⸻

###Google Firebase

Pros
* Tight integration of services.
* Good for rapid prototyping.
* Strong support for real-time features.

Cons
* Higher learning curve.
* Less straightforward to version-control configuration.
* Limited flexibility for complex integrations.