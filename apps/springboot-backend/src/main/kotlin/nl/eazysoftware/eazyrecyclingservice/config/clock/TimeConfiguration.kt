package nl.eazysoftware.eazyrecyclingservice.config.clock

import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Centralized time configuration for the application.
 *
 * **Policy:**
 * - All domain logic and storage uses UTC (enforced by Application.kt setting JVM default)
 * - Display timezone is Europe/Amsterdam (CET/CEST) for user-facing timestamps
 * - Calendar arithmetic (years, months) uses UTC to avoid DST issues
 *
 * **Note:** The JVM default timezone is set to UTC in Application.kt via TimeZone.setDefault().
 * This affects all java.time operations, Hibernate, and Jackson serialization.
 */
object TimeConfiguration {
  /**
   * UTC timezone for all domain calculations and storage.
   * Use this for calendar arithmetic (adding years, months, days).
   */
  val DOMAIN_TIMEZONE: TimeZone = TimeZone.UTC


  /**
   * Display timezone for user-facing timestamps (frontend, reports, logs).
   * Europe/Amsterdam handles CET (UTC+1) and CEST (UTC+2) automatically.
   *
   * Use this when converting UTC timestamps to local time for display purposes.
   */
  val DISPLAY_TIMEZONE_KX: TimeZone = TimeZone.of("Europe/Amsterdam")

  /**
   * Java ZoneId equivalent of DISPLAY_TIMEZONE_KX for use with java.time APIs.
   */
  val DISPLAY_ZONE_ID: ZoneId = ZoneId.of("Europe/Amsterdam")
}

/**
 * Calculate month boundaries in display timezone (Europe/Amsterdam) for business logic queries.
 * Returns UTC Instants suitable for database queries.
 *
 * This ensures that month boundaries are calculated based on CET/CEST timezone,
 * preventing issues where weight tickets entered on the last day of a month in CET
 * are incorrectly included in the next month's declarations due to UTC conversion.
 *
 * @return Pair of (startOfMonth, endOfMonth) as UTC Instants
 */
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
