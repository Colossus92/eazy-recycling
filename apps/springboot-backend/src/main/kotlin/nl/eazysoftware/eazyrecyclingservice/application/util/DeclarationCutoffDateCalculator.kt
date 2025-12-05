package nl.eazysoftware.eazyrecyclingservice.application.util

import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import nl.eazysoftware.eazyrecyclingservice.config.clock.toYearMonth

/**
 * Utility for calculating declaration cutoff dates.
 *
 * The declaration deadline is the 20th of the following month. For example:
 * - October weight tickets have a deadline of November 20th
 * - On December 4th: October and earlier are processed (November's deadline hasn't passed)
 * - On December 21st: November and earlier are processed (November's deadline passed on December 20th)
 */
object DeclarationCutoffDateCalculator {

  /**
   * Calculates the cutoff date for late declarations.
   *
   * Only weight tickets from months that have passed their declaration deadline (the 20th of the following month)
   * should be processed.
   *
   * - If current day is before the 20th: cutoff = start of (current month - 1)
   * - If current day is on or after the 20th: cutoff = start of current month
   */
  fun calculateDeclarationCutoffDate(now: kotlin.time.Instant): YearMonth {
    val currentYearMonth = now.toYearMonth()

    // Get the day of month from the instant
    val epochMillis = now.toEpochMilliseconds()
    val javaInstant = java.time.Instant.ofEpochMilli(epochMillis)
    val zonedDateTime = javaInstant.atZone(java.time.ZoneId.of("Europe/Amsterdam"))
    val dayOfMonth = zonedDateTime.dayOfMonth

    return if (dayOfMonth < 20) {
      // Before the 20th: only process months before the previous month
      // e.g., on December 4th, cutoff is November 1st (so October and earlier are included)
      currentYearMonth.minusMonths(1)
    } else {
      // On or after the 20th: process up to the previous month
      // e.g., on December 21st, cutoff is December 1st (so November and earlier are included)
      currentYearMonth
    }
  }

  private fun YearMonth.minusMonths(months: Int): YearMonth {
    var year = this.year
    var monthValue = this.month.number - months
    while (monthValue <= 0) {
      monthValue += 12
      year -= 1
    }
    return YearMonth(year, monthValue)
  }
}
