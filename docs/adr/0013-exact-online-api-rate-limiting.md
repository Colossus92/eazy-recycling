# ADR-0013: Exact Online API Rate Limiting Compliance

## Status

Proposed

## Context and Problem Statement

Exact Online enforces strict API rate limits to ensure platform stability. Our application must comply with these limits to prevent being blocked from API access. Currently, our `ExactOnlineSyncAdapter` makes API calls without rate limiting awareness, which could lead to:

1. HTTP 429 (Too Many Requests) responses
2. Temporary or extended API access blocks
3. Poor user experience when sync operations fail unexpectedly

### Exact Online API Limits

| Limit Type | Threshold | Window | Consequence |
|------------|-----------|--------|-------------|
| **Minutely limit** | 60 calls | Per minute, per company (division) | HTTP 429, blocked for remainder of minute |
| **Daily limit** | 5,000 calls (new apps) / 50,000 (legacy) | Per day, per company | HTTP 429, blocked until next day |
| **Token endpoint** | 200 calls | Per API key, per user, per day | Blocked for token requests |
| **Error limit** | 10 errors | Per API key, per user, per company, per endpoint, per hour | Temporary block, escalating with repeated violations |

### Response Headers

Exact Online returns rate limit information in response headers:

| Header | Description |
|--------|-------------|
| `X-RateLimit-Minutely-Remaining` | Remaining calls in current minute |
| `X-RateLimit-Minutely-Reset` | Unix timestamp when minutely limit resets |
| `X-RateLimit-Limit` | Daily limit |
| `X-RateLimit-Remaining` | Remaining daily calls |
| `X-RateLimit-Reset` | Unix timestamp when daily limit resets |

## Considered Options

### Option 1: Reactive Rate Limiting (Handle 429 responses)

Catch HTTP 429 responses and implement exponential backoff retry.

Pros:

- Simple to implement
- No proactive tracking needed

Cons:

- Still hits the API limit (counts against quota)
- User experiences delays after limit is hit
- Risk of escalating blocks with repeated 429s

### Option 2: Proactive Rate Limiting with Spring Boot Native Tools

Use Spring's `RestClient` interceptors and a rate limiter (Bucket4j or Resilience4j) to prevent exceeding limits before they occur.

Pros:

- Prevents hitting limits entirely
- Clear user feedback before operations
- Uses Spring Boot ecosystem
- Can track remaining quota from response headers

Cons:

- More complex implementation
- Requires state management for rate limit tracking

### Option 3: Hybrid Approach (Proactive + Reactive)

Combine proactive rate limiting with reactive 429 handling as a safety net.

Pros:

- Best of both worlds
- Graceful degradation
- Robust error handling

Cons:

- Most complex implementation

## Decision Outcome

**Chosen option: Option 3 - Hybrid Approach**

This provides the most robust solution: proactive limiting prevents most issues, while reactive handling provides a safety net for edge cases (e.g., other applications sharing the same API quota).

## Implementation Strategy

### 1. Rate Limiter Configuration

Use **Resilience4j** (already common in Spring Boot ecosystem) for rate limiting:

```kotlin
// ExactOnlineRateLimiterConfig.kt
@Configuration
class ExactOnlineRateLimiterConfig {
    
    @Bean
    fun exactOnlineRateLimiter(): RateLimiter {
        val config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(55) // Leave 5 call buffer below 60 limit
            .timeoutDuration(Duration.ofSeconds(30))
            .build()
        
        return RateLimiter.of("exactOnline", config)
    }
}
```

### 2. Rate Limit Tracking Service

Track remaining quota from response headers:

```kotlin
// ExactOnlineRateLimitTracker.kt
@Component
class ExactOnlineRateLimitTracker {
    
    private val minutelyRemaining = AtomicInteger(60)
    private val dailyRemaining = AtomicInteger(5000)
    private val minutelyResetAt = AtomicLong(0)
    private val dailyResetAt = AtomicLong(0)
    
    fun updateFromHeaders(headers: HttpHeaders) {
        headers["X-RateLimit-Minutely-Remaining"]?.firstOrNull()?.toIntOrNull()
            ?.let { minutelyRemaining.set(it) }
        headers["X-RateLimit-Remaining"]?.firstOrNull()?.toIntOrNull()
            ?.let { dailyRemaining.set(it) }
        headers["X-RateLimit-Minutely-Reset"]?.firstOrNull()?.toLongOrNull()
            ?.let { minutelyResetAt.set(it) }
        headers["X-RateLimit-Reset"]?.firstOrNull()?.toLongOrNull()
            ?.let { dailyResetAt.set(it) }
    }
    
    fun canMakeRequest(): RateLimitStatus {
        val now = Instant.now().epochSecond
        
        // Check if limits have reset
        if (now >= minutelyResetAt.get()) {
            minutelyRemaining.set(60)
        }
        if (now >= dailyResetAt.get()) {
            dailyRemaining.set(5000)
        }
        
        return when {
            dailyRemaining.get() <= 0 -> RateLimitStatus.DailyLimitExceeded(
                resetAt = Instant.ofEpochSecond(dailyResetAt.get())
            )
            minutelyRemaining.get() <= 0 -> RateLimitStatus.MinutelyLimitExceeded(
                resetAt = Instant.ofEpochSecond(minutelyResetAt.get())
            )
            else -> RateLimitStatus.Ok(
                minutelyRemaining = minutelyRemaining.get(),
                dailyRemaining = dailyRemaining.get()
            )
        }
    }
}

sealed class RateLimitStatus {
    data class Ok(val minutelyRemaining: Int, val dailyRemaining: Int) : RateLimitStatus()
    data class MinutelyLimitExceeded(val resetAt: Instant) : RateLimitStatus()
    data class DailyLimitExceeded(val resetAt: Instant) : RateLimitStatus()
}
```

### 3. RestClient Interceptor

Intercept all Exact Online API calls:

```kotlin
// ExactOnlineRateLimitInterceptor.kt
@Component
class ExactOnlineRateLimitInterceptor(
    private val rateLimitTracker: ExactOnlineRateLimitTracker,
    private val rateLimiter: RateLimiter
) : ClientHttpRequestInterceptor {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        // Check proactive rate limit
        when (val status = rateLimitTracker.canMakeRequest()) {
            is RateLimitStatus.DailyLimitExceeded -> {
                throw ExactOnlineRateLimitException(
                    "Daily API limit exceeded. Resets at ${status.resetAt}",
                    status.resetAt
                )
            }
            is RateLimitStatus.MinutelyLimitExceeded -> {
                throw ExactOnlineRateLimitException(
                    "Minutely API limit exceeded. Resets at ${status.resetAt}",
                    status.resetAt
                )
            }
            is RateLimitStatus.Ok -> {
                logger.debug("Rate limit OK: ${status.minutelyRemaining}/min, ${status.dailyRemaining}/day remaining")
            }
        }
        
        // Apply Resilience4j rate limiter
        return RateLimiter.decorateCheckedSupplier(rateLimiter) {
            val response = execution.execute(request, body)
            
            // Update tracker from response headers
            rateLimitTracker.updateFromHeaders(response.headers)
            
            // Handle 429 response
            if (response.statusCode == HttpStatus.TOO_MANY_REQUESTS) {
                val retryAfter = response.headers["Retry-After"]?.firstOrNull()?.toLongOrNull() ?: 60
                throw ExactOnlineRateLimitException(
                    "Rate limit exceeded (HTTP 429)",
                    Instant.now().plusSeconds(retryAfter)
                )
            }
            
            response
        }.get()
    }
}
```

### 4. Custom Exception for Rate Limiting

```kotlin
// ExactOnlineRateLimitException.kt
class ExactOnlineRateLimitException(
    message: String,
    val resetAt: Instant
) : RuntimeException(message) {
    
    fun getWaitDuration(): Duration = Duration.between(Instant.now(), resetAt)
    
    fun getUserFriendlyMessage(): String {
        val waitSeconds = getWaitDuration().seconds
        return when {
            waitSeconds <= 60 -> "Exact Online API limiet bereikt. Probeer over $waitSeconds seconden opnieuw."
            waitSeconds <= 3600 -> "Exact Online API limiet bereikt. Probeer over ${waitSeconds / 60} minuten opnieuw."
            else -> "Exact Online dagelijkse API limiet bereikt. Probeer morgen opnieuw."
        }
    }
}
```

### 5. Controller Exception Handler

Return clear error responses to the frontend:

```kotlin
// ExactOnlineExceptionHandler.kt
@RestControllerAdvice
class ExactOnlineExceptionHandler {
    
    @ExceptionHandler(ExactOnlineRateLimitException::class)
    fun handleRateLimitException(ex: ExactOnlineRateLimitException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", ex.getWaitDuration().seconds.toString())
            .body(ErrorResponse(
                error = "RATE_LIMIT_EXCEEDED",
                message = ex.getUserFriendlyMessage(),
                retryAfterSeconds = ex.getWaitDuration().seconds
            ))
    }
}

data class ErrorResponse(
    val error: String,
    val message: String,
    val retryAfterSeconds: Long
)
```

### 6. Frontend Integration

The frontend should handle the 429 response and display a user-friendly message:

```typescript
// In API error handler
if (error.response?.status === 429) {
  const retryAfter = error.response.data.retryAfterSeconds;
  const message = error.response.data.message;
  toastService.error(message);
  // Optionally disable sync button for retryAfter seconds
}
```

### 7. Sync Operation Batching

For bulk sync operations, implement batching with delays:

```kotlin
// In ExactOnlineSyncAdapter
private suspend fun syncWithRateLimiting(accounts: List<ExactSyncAccount>) {
    val batchSize = 50 // Process 50 at a time
    val delayBetweenBatches = Duration.ofSeconds(1)
    
    accounts.chunked(batchSize).forEach { batch ->
        batch.forEach { account ->
            processExactAccount(account)
        }
        delay(delayBetweenBatches.toMillis())
    }
}
```

## Best Practices Summary

| Practice | Implementation |
|----------|----------------|
| **Track rate limits** | Read `X-RateLimit-*` headers from every response |
| **Proactive limiting** | Check remaining quota before making requests |
| **Buffer margin** | Keep 5-10% buffer below actual limits |
| **Exponential backoff** | On 429, wait with increasing delays |
| **User feedback** | Show clear messages when limits are hit |
| **Batch operations** | Process bulk syncs in smaller batches with delays |
| **Use Sync API** | Prefer Sync API over bulk endpoints (no rate limit on Sync API) |
| **Minimize calls** | Only sync changed records using timestamps |
| **Log API usage** | Track call counts for monitoring |

## Dependencies

Add to `build.gradle.kts`:

```kotlin
implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
implementation("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")
```

## Consequences

### Positive

- Prevents API access blocks
- Clear user feedback when limits are approached
- Graceful degradation under load
- Audit trail of API usage
- Uses Spring Boot native patterns

### Negative

- Additional complexity in API client layer
- State management for rate limit tracking
- Potential delays in sync operations when limits are approached

### Neutral

- Requires monitoring of rate limit metrics
- May need adjustment as Exact Online changes limits

## References

- [Exact Online API Limits Documentation](https://support.exactonline.com/community/s/knowledge-base#All-All-DNO-Simulation-gen-apilimits)
- [Resilience4j Rate Limiter](https://resilience4j.readme.io/docs/ratelimiter)
- [Spring RestClient Interceptors](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html)
- ADR-0012: Exact Online Company Synchronization Strategy
