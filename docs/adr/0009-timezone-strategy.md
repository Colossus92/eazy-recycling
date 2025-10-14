# Timezone Strategy for Eazy Recycling

## Policy: Store UTC, Display Local

### Core Principles

1. **All timestamps stored in UTC** - Database, domain models, and business logic use UTC
2. **Display in Europe/Amsterdam** - Frontend shows times in CET/CEST for users
3. **Calendar arithmetic in UTC** - Adding years/months uses UTC to avoid DST issues

## Why UTC for Domain Logic?

### Problem with CET/CEST
```kotlin
// ❌ WRONG: CET has DST transitions
val instant = Instant.parse("2025-03-30T01:30:00Z") // Night before DST
val fiveYearsLater = instant.plus(5, DateTimeUnit.YEAR, TimeZone.of("Europe/Amsterdam"))
// This can cause issues: 2:00 AM doesn't exist on DST transition day!
```

### Solution with UTC
```kotlin
// ✅ CORRECT: UTC has no DST transitions
val instant = Instant.parse("2025-03-30T01:30:00Z")
val fiveYearsLater = instant.plus(5, DateTimeUnit.YEAR, TimeZone.UTC)
// Always unambiguous, always correct
```

## Implementation

### 1. JVM Default Timezone (Application.kt)
```kotlin
@SpringBootApplication
class Application {
    @PostConstruct
    fun init() {
        // Set JVM default timezone to UTC for consistency
        // This affects all java.time operations, Hibernate, and Jackson
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
}
```

### 2. Configuration Constants (TimeConfiguration.kt)
```kotlin
object TimeConfiguration {
    // For domain logic and calendar arithmetic
    val DOMAIN_TIMEZONE: TimeZone = TimeZone.UTC
    
    // For display/UI purposes only
    val DISPLAY_TIMEZONE: ZoneId = ZoneId.of("Europe/Amsterdam")
    val DISPLAY_TIMEZONE_KX: TimeZone = TimeZone.of("Europe/Amsterdam")
}
```

**Important:** `DISPLAY_TIMEZONE` is ONLY for converting UTC to local time for display. 
All business logic uses UTC via `DOMAIN_TIMEZONE`.

### 3. Spring Configuration (application.yaml)
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc.time_zone: UTC
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
```

## Usage Examples

### Domain Logic - Calendar Arithmetic
```kotlin
import nl.eazysoftware.eazyrecyclingservice.config.TimeConfiguration

// Adding years (calendar-based)
val expiryDate = lastActivityAt.plus(5, DateTimeUnit.YEAR, TimeConfiguration.DOMAIN_TIMEZONE)

// Adding days (calendar-based)
val deadline = createdAt.plus(30, DateTimeUnit.DAY, TimeConfiguration.DOMAIN_TIMEZONE)

// Adding duration (time-based, no timezone needed)
val timeout = requestTime.plus(5.minutes)
```

### Converting for Display
```kotlin
import kotlinx.datetime.*

// Convert Instant to LocalDateTime for display
fun Instant.toDisplayTime(): LocalDateTime {
    return this.toLocalDateTime(TimeConfiguration.DISPLAY_TIMEZONE_KX)
}

// Example usage
val displayTime = wasteStream.lastActivityAt.toDisplayTime()
println("Last activity: ${displayTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))}")
```

### REST API - JSON Serialization
```kotlin
// Jackson automatically serializes to ISO-8601 in UTC
data class WasteStreamResponse(
    val wasteStreamNumber: String,
    val lastActivityAt: Instant, // Serializes as "2025-10-14T12:49:19Z"
    val status: String
)
```

### Frontend Display (React)
```typescript
// Frontend converts UTC to local browser timezone
const displayTime = new Date(wasteStream.lastActivityAt).toLocaleString('nl-NL', {
  timeZone: 'Europe/Amsterdam',
  dateStyle: 'medium',
  timeStyle: 'short'
});
// Shows: "14 okt 2025, 14:49" (in CET/CEST)
```

## Type Mapping Strategy

### Domain Layer → kotlinx.datetime.Instant
```kotlin
// WasteStream.kt (domain model)
data class WasteStream(
    val lastActivityAt: kotlinx.datetime.Instant
)
```

### Persistence Layer → java.time.Instant
```kotlin
// WasteStreamDto.kt (JPA entity)
@Entity
data class WasteStreamDto(
    @Column(name = "last_activity_at")
    val lastActivityAt: java.time.Instant
)
```

### Conversion at Repository Boundary
```kotlin
// WasteStreamMapper.kt
import nl.eazysoftware.eazyrecyclingservice.config.toJavaInstant
import nl.eazysoftware.eazyrecyclingservice.config.toKotlinxInstant

fun toDomain(dto: WasteStreamDto): WasteStream {
    return WasteStream(
        lastActivityAt = dto.lastActivityAt.toKotlinxInstant()
    )
}

fun toDto(domain: WasteStream): WasteStreamDto {
    return WasteStreamDto(
        lastActivityAt = domain.lastActivityAt.toJavaInstant()
    )
}
```

## Database Schema

PostgreSQL stores `TIMESTAMP WITH TIME ZONE` in UTC internally:

```sql
CREATE TABLE waste_streams (
    number VARCHAR(12) PRIMARY KEY,
    last_activity_at TIMESTAMP WITH TIME ZONE NOT NULL,
    -- Hibernate maps java.time.Instant to TIMESTAMP WITH TIME ZONE
    -- Always stored as UTC, no offset information
);
```

### Why Not OffsetDateTime?

❌ **Don't use `OffsetDateTime`**:
- Stores offset (`+01:00`, `+02:00`) but not timezone name
- Can't determine if it's CET or CEST from offset alone
- Makes DST transitions ambiguous
- Unnecessary complexity for business logic

✅ **Use `Instant` instead**:
- Represents absolute point in time
- No ambiguity, always UTC
- Works seamlessly with calendar arithmetic
- Hibernate has native support for `java.time.Instant`

## Testing

### Unit Tests
```kotlin
@Test
fun `should expire after 5 years`() {
    val now = Instant.parse("2025-10-14T12:00:00Z")
    val lastActivity = Instant.parse("2020-10-14T12:00:00Z")
    
    val status = EffectiveStatusPolicy.compute(
        WasteStreamStatus.ACTIVE,
        lastActivity,
        now
    )
    
    assertEquals(EffectiveStatus.EXPIRED, status)
}
```

### Integration Tests
```kotlin
@Test
fun `should handle DST transitions correctly`() {
    // Test around DST transition (last Sunday of March)
    val beforeDST = Instant.parse("2025-03-29T23:00:00Z")
    val afterDST = beforeDST.plus(5, DateTimeUnit.YEAR, TimeConfiguration.DOMAIN_TIMEZONE)
    
    // Should be exactly 5 years later, no DST weirdness
    assertEquals("2030-03-29T23:00:00Z", afterDST.toString())
}
```

## Common Pitfalls to Avoid

### ❌ Don't Use Local Timezone for Business Logic
```kotlin
// WRONG: Business logic should not depend on local timezone
val expiry = lastActivity.plus(5, DateTimeUnit.YEAR, TimeZone.currentSystemDefault())
```

### ❌ Don't Mix java.time and kotlinx.datetime Carelessly
```kotlin
// WRONG: Mixing without proper conversion
val javaInstant = java.time.Instant.now()
val kotlinInstant = Instant.fromEpochMilliseconds(javaInstant.toEpochMilli())
```

### ✅ Do Use Extension Functions for Conversions
```kotlin
fun java.time.Instant.toKotlinInstant(): Instant = 
    Instant.fromEpochMilliseconds(this.toEpochMilli())

fun Instant.toJavaInstant(): java.time.Instant = 
    java.time.Instant.ofEpochMilli(this.toEpochMilliseconds())
```

## References

- [kotlinx.datetime Documentation](https://github.com/Kotlin/kotlinx-datetime)
- [Spring Boot Timezone Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
- [PostgreSQL Timezone Handling](https://www.postgresql.org/docs/current/datatype-datetime.html)
