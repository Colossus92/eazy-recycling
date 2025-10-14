package nl.eazysoftware.eazyrecyclingservice.config.clock

import kotlinx.datetime.TimeZone

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
}
