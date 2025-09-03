# Context and Problem Statement

We need to generate waybill PDFs and send them to recipients. This is a relatively heavy task that would put unnecessary load on the Spring Boot backend. The solution should be asynchronous, scalable, and not block the main application.

## Considered Options
* Supabase Edge Functions
* AWS Lambda
* DigitalOcean Functions
* Spring Boot backend handling tasks directly

## Decision Outcome

Chosen option: Supabase Edge Functions.
Supabase provides first-class support for edge functions written in Deno. This allows us to easily offload heavy asynchronous tasks such as PDF generation and email delivery. The integration with Supabase services makes it straightforward to implement, while avoiding the complexity of introducing additional platforms at this stage.

Although there are concerns online about performance limits, this is not a critical issue because the tasks are asynchronous and do not have strict time constraints.

## Pros and Cons of the Options

### Supabase Edge Functions

Pros
* Simple to set up within our existing Supabase environment.
* Allows offloading heavy tasks from the backend.
* Good integration with Supabase services (auth, storage, database).
* Cost-effective, included in current Supabase usage.

Cons
* Based on Deno, which has a smaller ecosystem compared to Node.js.
* Some concerns about performance at scale.

⸻

### AWS Lambda

Pros
* Mature, widely adopted serverless platform.
* Rich ecosystem and scaling capabilities.

Cons
* Requires introducing AWS into our stack.
* More complex setup and additional operational overhead.
* Higher costs compared to current needs.

⸻

### DigitalOcean Functions

Pros
* Fits within existing DigitalOcean environment.
* Reasonable pricing and scalability.

Cons
* Less tightly integrated with Supabase.
* Requires extra effort to connect with Supabase services.

⸻

### Spring Boot backend handling tasks directly

Pros
* No external dependencies required.
* Reuse existing backend stack.

Cons
* Adds heavy, asynchronous load to the backend.
* Less scalable and harder to isolate from core logic.
* Would impact performance of synchronous requests.
