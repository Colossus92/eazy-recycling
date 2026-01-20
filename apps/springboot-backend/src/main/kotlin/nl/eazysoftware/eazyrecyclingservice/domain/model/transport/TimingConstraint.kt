package nl.eazysoftware.eazyrecyclingservice.domain.model.transport

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Enum representing the timing mode for transport scheduling.
 * Used for VRPTW (Vehicle Routing Problem with Time Windows) constraints.
 */
enum class TimingMode {
  /**
   * Only a date is specified, no specific time window.
   * The transport can occur at any time during the specified date.
   */
  DATE_ONLY,

  /**
   * A time window is specified with different start and end times.
   * The transport should occur within this window.
   */
  WINDOW,

  /**
   * A fixed appointment time where start equals end.
   * The transport should occur at this exact time.
   */
  FIXED
}

/**
 * Represents a timing constraint for pickup or delivery scheduling.
 * This value object encapsulates the scheduling requirements for transport logistics.
 */
data class TimingConstraint(
  val date: LocalDate,
  val mode: TimingMode,
  val windowStart: LocalTime?,
  val windowEnd: LocalTime?
) {
  init {
    validate()
  }

  private fun validate() {
    when (mode) {
      TimingMode.DATE_ONLY -> {
        // windowStart and windowEnd should be null for DATE_ONLY mode
        // We allow them to be non-null but ignore them
      }
      TimingMode.WINDOW, TimingMode.FIXED -> {
        requireNotNull(windowStart) { "windowStart must not be null when mode is $mode" }
        requireNotNull(windowEnd) { "windowEnd must not be null when mode is $mode" }
        require(windowStart <= windowEnd) {
          "windowStart ($windowStart) must be less than or equal to windowEnd ($windowEnd)"
        }
      }
    }
  }

  /**
   * Converts the timing constraint to an Instant using the specified timezone.
   * For DATE_ONLY mode, returns the start of the day.
   * For WINDOW and FIXED modes, returns the windowStart time.
   */
  fun toInstant(timeZone: TimeZone = TimeZone.of("Europe/Amsterdam")): Instant {
    val time = when (mode) {
      TimingMode.DATE_ONLY -> LocalTime(0, 0)
      TimingMode.WINDOW, TimingMode.FIXED -> windowStart ?: LocalTime(0, 0)
    }
    return date.atTime(time).toInstant(timeZone)
  }

  /**
   * Returns the end instant for window-based constraints.
   * For DATE_ONLY mode, returns end of day (23:59).
   * For WINDOW and FIXED modes, returns the windowEnd time.
   */
  fun toEndInstant(timeZone: TimeZone = TimeZone.of("Europe/Amsterdam")): Instant {
    val time = when (mode) {
      TimingMode.DATE_ONLY -> LocalTime(23, 59)
      TimingMode.WINDOW, TimingMode.FIXED -> windowEnd ?: LocalTime(23, 59)
    }
    return date.atTime(time).toInstant(timeZone)
  }

  companion object {
    /**
     * Creates a DATE_ONLY timing constraint for the specified date.
     */
    fun dateOnly(date: LocalDate): TimingConstraint {
      return TimingConstraint(
        date = date,
        mode = TimingMode.DATE_ONLY,
        windowStart = null,
        windowEnd = null
      )
    }

    /**
     * Creates a WINDOW timing constraint with the specified date and time window.
     */
    fun window(date: LocalDate, start: LocalTime, end: LocalTime): TimingConstraint {
      return TimingConstraint(
        date = date,
        mode = TimingMode.WINDOW,
        windowStart = start,
        windowEnd = end
      )
    }

    /**
     * Creates a FIXED timing constraint for a specific appointment time.
     */
    fun fixed(date: LocalDate, time: LocalTime): TimingConstraint {
      return TimingConstraint(
        date = date,
        mode = TimingMode.FIXED,
        windowStart = time,
        windowEnd = time
      )
    }

    /**
     * Creates a timing constraint from a legacy Instant value.
     * Converts to a FIXED constraint at the specified time.
     */
    fun fromInstant(instant: Instant, timeZone: TimeZone = TimeZone.of("Europe/Amsterdam")): TimingConstraint {
      val localDateTime = instant.toKotlinxLocalDateTime(timeZone)
      return fixed(localDateTime.date, localDateTime.time)
    }

    /**
     * Determines the mode based on the provided time values.
     */
    fun determineMode(windowStart: LocalTime?, windowEnd: LocalTime?): TimingMode {
      return when {
        windowStart == null || windowEnd == null -> TimingMode.DATE_ONLY
        windowStart == windowEnd -> TimingMode.FIXED
        else -> TimingMode.WINDOW
      }
    }
  }
}

private fun Instant.toKotlinxLocalDateTime(timeZone: TimeZone): kotlinx.datetime.LocalDateTime {
  val kotlinxInstant = kotlinx.datetime.Instant.fromEpochMilliseconds(this.toEpochMilliseconds())
  return kotlinxInstant.toLocalDateTime(timeZone)
}
