package nl.eazysoftware.eazyrecyclingservice.config.clock

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Extension functions for converting between java.time.Instant (JPA) and kotlinx.datetime.Instant (domain).
 *
 * **Usage:**
 * - Use `kotlinx.datetime.Instant` in domain models (WasteStream)
 * - Convert at the repository boundary using these extensions
 */


fun Instant.toDisplayTime(): LocalDateTime {
  return this.toLocalDateTime(TimeConfiguration.DISPLAY_TIMEZONE_KX)
}

fun Instant.toDisplayString(): String {
  return this.toDisplayTime().toString()
}
