# Retry Strategy for Exact Online Maintenance Windows

## Context and Problem Statement

The Exact Online token refresh job runs via `ExactTokenRefreshScheduler` and dynamically schedules token refreshes 25 seconds before expiry. However, Exact Online has scheduled maintenance windows every night from 04:00-04:30 CET during which the API returns 503/504 errors with the message "Server maintenance: Due to server maintenance the Exact Online server is unavailable."

When the scheduled token refresh coincidentally occurs during this maintenance window (which happens frequently as tokens typically expire during the night), the refresh fails and the application loses its valid access token until manual intervention or a successful retry occurs.

The current implementation has a simple retry mechanism (reschedule after 30 seconds), but it doesn't:
- Distinguish between transient maintenance errors (503/504) and permanent failures (401, 403, etc.)
- Use exponential backoff to avoid overwhelming the API when it comes back online
- Follow Spring Boot best practices for retry logic

How should we implement a robust retry strategy that handles Exact Online maintenance windows gracefully?

## Considered options

* **Option 1: Manual retry logic with Thread.sleep**
* **Option 2: Spring Retry with @Retryable annotation**
* **Option 3: Resilience4j Circuit Breaker pattern**
* **Option 4: Keep current simple retry (reschedule after 30s)**

## Decision Outcome

Chosen option: "**Spring Retry with @Retryable annotation**", because:

- It's the Spring Boot idiomatic approach for retry logic
- Provides declarative retry configuration via annotations
- Built-in exponential backoff support
- Allows fine-grained control over which exceptions to retry
- Integrates seamlessly with existing Spring components
- Simpler to test and maintain than manual retry logic
- Well-documented and widely used in Spring ecosystem

The maintenance window is 30 minutes (04:00-04:30 CET), so we need a retry strategy that:
- Retries only on HTTP 503 (Service Unavailable) and 504 (Gateway Timeout)
- Uses exponential backoff starting at 1 minute
- Retries for up to ~1 hour total (to cover the 30-minute window + generous buffer)
- Throws exceptions immediately for other error types (401, 403, etc.)

## Pros and Cons of the Options

### Option 1: Manual retry logic with Thread.sleep

**Pros:**
- No additional dependencies
- Full control over retry behavior
- Simple to understand

**Cons:**
- Blocks scheduler threads during sleep
- Reinventing the wheel
- Harder to test
- More boilerplate code
- No declarative configuration

### Option 2: Spring Retry with @Retryable annotation (CHOSEN)

**Pros:**
- Declarative and clean
- Built-in exponential backoff
- Easy to configure retry conditions
- Non-blocking (uses scheduling under the hood)
- Well-tested framework
- Easy to add metrics/logging via listeners
- Follows Spring Boot best practices

**Cons:**
- Adds a small dependency (~100KB)
- Requires understanding Spring Retry concepts

### Option 3: Resilience4j Circuit Breaker pattern

**Pros:**
- More sophisticated failure handling
- Circuit breaker prevents cascading failures
- Rich metrics and monitoring
- Modern resilience library

**Cons:**
- Overkill for this use case (single scheduled job, not user-facing)
- Larger dependency footprint
- Circuit breaker doesn't fit maintenance window pattern
- More complex configuration

### Option 4: Keep current simple retry (reschedule after 30s)

**Pros:**
- No changes needed
- Works for most cases

**Cons:**
- Doesn't distinguish between error types
- Fixed 30s delay isn't optimal for 30-minute maintenance windows
- Will fail multiple times during maintenance window
- Doesn't follow exponential backoff best practices
- May cause token expiry if maintenance window is long

## Implementation Details

### Spring Retry Configuration

```kotlin
@Retryable(
    retryFor = [ExactMaintenanceException::class],
    maxAttempts = 10, // ~63 minutes total with exponential backoff
    backoff = Backoff(
        delay = 60000,        // Start at 1 minute
        multiplier = 1.5,     // Exponential increase
        maxDelay = 600000     // Cap at 10 minutes
    )
)
```

### Retry Schedule

With exponential backoff (multiplier 1.5, capped at 10 minutes):
1. Attempt 1: Immediate
2. Attempt 2: +1 min = 1 min
3. Attempt 3: +1.5 min = 2.5 min
4. Attempt 4: +2.25 min = 4.75 min
5. Attempt 5: +3.375 min = 8.125 min
6. Attempt 6: +5.06 min = 13.19 min
7. Attempt 7: +7.59 min = 20.78 min
8. Attempt 8: +10 min (capped) = 30.78 min
9. Attempt 9: +10 min (capped) = 40.78 min
10. Attempt 10: +10 min (capped) = 50.78 min
11. Attempt 11: +10 min (capped) = 60.78 min

Total: ~63 minutes, covering the 30-minute maintenance window with generous buffer.

### Error Classification

- **Retry (503, 504)**: Transient errors indicating maintenance or temporary unavailability
- **Don't Retry (400, 401, 403, etc.)**: Permanent errors requiring manual intervention (bad credentials, expired refresh token, etc.)
- **Retry (IllegalStateException)**: Transient errors indicating maintenance or temporary unavailability. On 12-01-2026 this exception occurred because the response from Exact Online was empty.

## More information

- Exact Online API documentation: https://support.exactonline.com/community/s/knowledge-base#All-All-DNO-Content-respcodeserrorhandling
- Spring Retry documentation: https://docs.spring.io/spring-retry/reference/
- Maintenance window: 04:00-04:30 CET (daily)
