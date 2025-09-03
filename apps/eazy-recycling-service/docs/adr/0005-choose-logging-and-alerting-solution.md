# ADR 0005: Choose Better Stack for Logging and Alerting

Context and Problem Statement

We need a solution for centralized logging and alerting to monitor application health and diagnose issues. While advanced observability platforms exist, they often come with high upfront costs and significant complexity to configure. At this stage, we need something easy to adopt, cost-effective, and sufficient for our current scale. Future growth may require reevaluating this choice.

## Considered Options
* Better Stack
* Datadog
* New Relic
* Grafana Cloud
* Self-hosted solutions (e.g., ELK stack, Prometheus + Alertmanager)

## Decision Outcome

Chosen option: Better Stack.
Better Stack provides an easy-to-use logging and alerting platform with a generous free plan. It allows us to get started quickly without incurring significant costs. As our usage grows, we will reevaluate based on costs and scaling needs.

## Pros and Cons of the Options

### Better Stack

Pros
* Very easy to set up and use.
* Generous free plan that covers early-stage needs.
* Good balance between functionality and simplicity.
* Hosted solution, no maintenance burden.

Cons
* May require switching or upgrading once usage and costs increase.
* Smaller ecosystem compared to larger providers.

⸻

### Datadog

Pros
* Comprehensive observability platform (logs, metrics, traces).
* Rich integrations and dashboards.
* Enterprise-grade scalability.

Cons
* Expensive from the start.
* More complex setup.

⸻

### New Relic

Pros
* Strong APM features.
* Wide ecosystem of integrations.

Cons
* Pricing can become steep quickly.
* More complex onboarding than Better Stack.

⸻

### Grafana Cloud

Pros
* Strong visualization and monitoring capabilities.
* Good integrations with Prometheus and Loki.

Cons
* Requires more setup effort than Better Stack.
* Free tier more limited for logging/alerting.

⸻

### Self-hosted solutions (ELK, Prometheus + Alertmanager, etc.)

Pros
* Full control over stack and configuration.
* No vendor lock-in.

Cons
* Significant setup and maintenance effort.
* Hosting and scaling costs shift to us.