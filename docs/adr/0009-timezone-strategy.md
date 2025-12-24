# Timezone Strategy for Eazy Recycling

## Status: Accepted

## Date: 2025-12-24

## Policy: Store UTC, Display CET/CEST, Business Logic in Display Timezone

### Core Principles

1. **All timestamps stored in UTC** - Database, domain models use UTC for storage
2. **Display in Europe/Amsterdam** - Frontend and logs show times in CET/CEST for users
3. **Calendar arithmetic in UTC** - Adding years/months uses UTC to avoid DST issues
4. **Business logic uses display timezone** - Month boundaries, date comparisons use CET/CEST to match user expectations

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
    
    // For display/UI purposes AND business logic that needs user timezone
    val DISPLAY_ZONE_ID: ZoneId = ZoneId.of("Europe/Amsterdam")
    val DISPLAY_TIMEZONE_KX: TimeZone = TimeZone.of("Europe/Amsterdam")
}

// Extension function for month boundaries in display timezone
fun YearMonth.toDisplayTimezoneBoundaries(): Pair<Instant, Instant> {
    val startOfMonthLocal = LocalDate.of(this.year, this.month.number, 1)
    val startOfMonth = startOfMonthLocal.atStartOfDay()
        .atZone(TimeConfiguration.DISPLAY_ZONE_ID)
        .toInstant()
    
    val endOfMonthLocal = if (this.month.number == 12) {
        LocalDate.of(this.year + 1, 1, 1)
    } else {
        LocalDate.of(this.year, this.month.number + 1, 1)
    }
    val endOfMonth = endOfMonthLocal.atStartOfDay()
        .atZone(TimeConfiguration.DISPLAY_ZONE_ID)
        .toInstant()
    
    return Pair(startOfMonth, endOfMonth)
}
```

**Important:** `DISPLAY_ZONE_ID` is used for:
1. Converting UTC to local time for display
2. Business logic that needs to match user expectations (e.g., month boundaries)

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

logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS}{Europe/Amsterdam} [%thread] %-5level %logger{36} - %msg%n"
```

### 4. Logging Configuration (logback-local.xml)

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss}{Europe/Amsterdam} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
</configuration>
```

**Note:** Logs display timestamps in CET/CEST for easier debugging and monitoring by users in the Europe/Amsterdam timezone.

## Business Logic Timezone Usage

### When to Use Display Timezone

Some business logic must use the display timezone (CET/CEST) to match user expectations:

#### Month Boundaries for Declarations

```kotlin
// Query weight tickets for November 2025 declarations
val yearMonth = YearMonth(2025, 11)
val (startOfMonth, endOfMonth) = yearMonth.toDisplayTimezoneBoundaries()

// startOfMonth = 2025-10-31T23:00:00Z (Nov 1 00:00 CET)
// endOfMonth = 2025-11-30T23:00:00Z (Dec 1 00:00 CET)

val weightTickets = repository.findByWeightedAtBetween(startOfMonth, endOfMonth)
```

**Why this matters:** A weight ticket entered on December 1st at 00:00 CET (November 30th 23:00 UTC) should NOT be included in November's declaration. Without timezone-aware boundaries, it would incorrectly be included because its UTC timestamp falls within November.

#### Cutoff Date Calculations

```kotlin
// Calculate declaration cutoff based on current day in CET
fun calculateDeclarationCutoffDate(now: Instant): YearMonth {
    val currentYearMonth = now.toYearMonth()
    
    // Get day of month in display timezone
    val zonedDateTime = java.time.Instant.ofEpochMilli(now.toEpochMilliseconds())
        .atZone(TimeConfiguration.DISPLAY_ZONE_ID)
    val dayOfMonth = zonedDateTime.dayOfMonth
    
    return if (dayOfMonth < 20) {
        currentYearMonth.minusMonths(1)
    } else {
        currentYearMonth
    }
}
```

### Extension Functions for Conversions

```kotlin
// Convert LocalDateTime (from user input) to UTC Instant
fun LocalDateTime.toCetKotlinInstant(): Instant {
    return this.atZone(TimeConfiguration.DISPLAY_ZONE_ID).toInstant().toKotlinInstant()
}

// Convert UTC Instant to LocalDateTime for display
fun java.time.Instant.toDisplayLocalDateTime(): LocalDateTime {
    return this.atZone(TimeConfiguration.DISPLAY_ZONE_ID).toLocalDateTime()
}

// Convert UTC Instant to LocalDate for display
fun java.time.Instant.toDisplayLocalDate(): LocalDate {
    return this.atZone(TimeConfiguration.DISPLAY_ZONE_ID).toLocalDate()
}
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

### Integration Tests - Timezone Boundaries

```kotlin
@Test
fun `should handle DST transitions correctly`() {
    // Test around DST transition (last Sunday of March)
    val beforeDST = Instant.parse("2025-03-29T23:00:00Z")
    val afterDST = beforeDST.plus(5, DateTimeUnit.YEAR, TimeConfiguration.DOMAIN_TIMEZONE)
    
    // Should be exactly 5 years later, no DST weirdness
    assertEquals("2030-03-29T23:00:00Z", afterDST.toString())
}

@Test
fun `should exclude weight tickets from next month when entered at midnight CET`() {
    // Weight ticket entered on December 1st 00:00 CET (November 30th 23:00 UTC)
    // This should NOT be included in November declarations
    val wasteStreamNumber = "087970000020"
    val yearMonth = YearMonth(2025, 11)
    
    createWasteStream(wasteStreamNumber, "ACTIVE")
    createWeightTicket(
        carrierPartyId = carrierCompanyId1,
        weightedAt = OffsetDateTime.of(2025, 11, 30, 23, 0, 0, 0, ZoneOffset.UTC), // Dec 1 00:00 CET
        lines = listOf(wasteStreamNumber to 1000.0)
    )
    
    val results = queryAdapter.findFirstReceivalDeclarations(yearMonth)
    
    assertThat(results).isEmpty()
}

@Test
fun `should include weight tickets from last second of month in CET`() {
    // Weight ticket entered on November 30th 23:59 CET (November 30th 22:59 UTC)
    // This SHOULD be included in November declarations
    val wasteStreamNumber = "087970000021"
    val yearMonth = YearMonth(2025, 11)
    
    createWasteStream(wasteStreamNumber, "ACTIVE")
    createWeightTicket(
        carrierPartyId = carrierCompanyId1,
        weightedAt = OffsetDateTime.of(2025, 11, 30, 22, 59, 0, 0, ZoneOffset.UTC), // Nov 30 23:59 CET
        lines = listOf(wasteStreamNumber to 1000.0)
    )
    
    val results = queryAdapter.findFirstReceivalDeclarations(yearMonth)
    
    assertThat(results).hasSize(1)
}
```

## Common Pitfalls to Avoid

### ❌ Don't Use Hardcoded Timezones

```kotlin
// WRONG: Hardcoded timezone string
val zonedTime = instant.atZone(ZoneId.of("Europe/Amsterdam"))

// CORRECT: Use centralized configuration
val zonedTime = instant.atZone(TimeConfiguration.DISPLAY_ZONE_ID)
```

### ❌ Don't Use UTC for Month Boundaries in Business Logic

```kotlin
// WRONG: Month boundaries in UTC don't match user expectations
val startOfMonth = LocalDate.of(2025, 11, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
val endOfMonth = LocalDate.of(2025, 12, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
// A weight ticket at Nov 30 23:30 UTC (Dec 1 00:30 CET) would be included in November!

// CORRECT: Use display timezone boundaries
val (startOfMonth, endOfMonth) = YearMonth(2025, 11).toDisplayTimezoneBoundaries()
// Now Nov 30 23:30 UTC is correctly excluded from November
```

### ❌ Don't Use Local Timezone for Calendar Arithmetic

```kotlin
// WRONG: Business logic should not depend on local timezone
val expiry = lastActivity.plus(5, DateTimeUnit.YEAR, TimeZone.currentSystemDefault())

// CORRECT: Use UTC for calendar arithmetic
val expiry = lastActivity.plus(5, DateTimeUnit.YEAR, TimeConfiguration.DOMAIN_TIMEZONE)
```

### ✅ Do Use Extension Functions for Conversions

```kotlin
// Centralized in JavaLocalDateTimeExtensions.kt
fun LocalDateTime.toCetKotlinInstant(): Instant {
    return this.atZone(TimeConfiguration.DISPLAY_ZONE_ID).toInstant().toKotlinInstant()
}

fun java.time.Instant.toDisplayLocalDateTime(): LocalDateTime {
    return this.atZone(TimeConfiguration.DISPLAY_ZONE_ID).toLocalDateTime()
}
```

## References

- [kotlinx.datetime Documentation](https://github.com/Kotlin/kotlinx-datetime)
- [Spring Boot Timezone Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
- [PostgreSQL Timezone Handling](https://www.postgresql.org/docs/current/datatype-datetime.html)
