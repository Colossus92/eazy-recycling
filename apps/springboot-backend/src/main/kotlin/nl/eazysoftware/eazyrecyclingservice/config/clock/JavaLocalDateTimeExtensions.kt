package nl.eazysoftware.eazyrecyclingservice.config.clock

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

/**
 * Converts a LocalDateTime (assumed to be in display timezone CET/CEST) to a Kotlin Instant (UTC).
 * Use this when converting user-entered datetime values to UTC for storage.
 */
fun LocalDateTime.toCetKotlinInstant(): Instant {
  return this.atZone(TimeConfiguration.DISPLAY_ZONE_ID).toInstant().toKotlinInstant()
}

/**
 * Converts a LocalDateTime (assumed to be in display timezone CET/CEST) to a Java Instant (UTC).
 * Use this when converting user-entered datetime values to UTC for storage.
 */
fun LocalDateTime.toCetInstant(): java.time.Instant {
  return this.atZone(TimeConfiguration.DISPLAY_ZONE_ID).toInstant()
}

/**
 * Converts a LocalDate (assumed to be in display timezone CET/CEST) to a Kotlin Instant (UTC)
 * representing the start of that day.
 * Use this when converting user-entered date values to UTC for storage/queries.
 */
fun LocalDate.toCetInstant(): Instant {
  return this.atStartOfDay(TimeConfiguration.DISPLAY_ZONE_ID).toInstant().toKotlinInstant()
}

/**
 * Converts a Java Instant (UTC) to a LocalDateTime in the display timezone (CET/CEST).
 * Use this when displaying UTC timestamps to users.
 */
fun java.time.Instant.toDisplayLocalDateTime(): LocalDateTime {
  return this.atZone(TimeConfiguration.DISPLAY_ZONE_ID).toLocalDateTime()
}

/**
 * Converts a Java Instant (UTC) to a LocalDate in the display timezone (CET/CEST).
 * Use this when displaying UTC timestamps as dates to users.
 */
fun java.time.Instant.toDisplayLocalDate(): LocalDate {
  return this.atZone(TimeConfiguration.DISPLAY_ZONE_ID).toLocalDate()
}

/**
 * Converts a Kotlin Instant (UTC) to a LocalDateTime in the display timezone (CET/CEST).
 * Use this when displaying UTC timestamps to users.
 */
fun Instant.toDisplayLocalDateTime(): LocalDateTime {
  return this.toJavaInstant().atZone(TimeConfiguration.DISPLAY_ZONE_ID).toLocalDateTime()
}
