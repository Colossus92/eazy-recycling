package nl.eazysoftware.eazyrecyclingservice.config.clock

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Instant as KotlinxInstant
import java.time.Instant as JavaInstant

/**
 * Extension functions for converting between java.time.Instant (JPA) and kotlinx.datetime.Instant (domain).
 *
 * **Usage:**
 * - Use `java.time.Instant` in JPA entities (WasteStreamDto)
 * - Use `kotlinx.datetime.Instant` in domain models (WasteStream)
 * - Convert at the repository boundary using these extensions
 */

/**
 * Converts kotlinx.datetime.Instant to java.time.Instant.
 * Used when mapping from domain model to DTO.
 */
fun KotlinxInstant.toJavaInstant(): JavaInstant =
    JavaInstant.ofEpochMilli(this.toEpochMilliseconds())

fun KotlinxInstant.toDisplayTime(): LocalDateTime {
  return this.toLocalDateTime(TimeConfiguration.DISPLAY_TIMEZONE_KX)
}
