package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TimingConstraint
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TimingMode
import java.time.format.DateTimeParseException
import kotlin.reflect.KClass

/**
 * Request DTO for timing constraint data from the frontend.
 * Supports three modes:
 * - DATE_ONLY: Only a date is specified
 * - WINDOW: A date with a time window (start and end times)
 * - FIXED: A fixed appointment time (start equals end)
 */
@ValidTimingConstraint
data class TimingConstraintRequest(
  val date: String,
  val mode: TimingMode,
  val windowStart: String? = null,
  val windowEnd: String? = null
) {
  /**
   * Converts this request to a domain TimingConstraint.
   */
  fun toDomain(): TimingConstraint {
    val parsedDate = LocalDate.parse(date)
    val parsedStart = windowStart?.let { parseTime(it) }
    val parsedEnd = windowEnd?.let { parseTime(it) }

    return TimingConstraint(
      date = parsedDate,
      mode = mode,
      windowStart = parsedStart,
      windowEnd = parsedEnd
    )
  }

  private fun parseTime(timeStr: String): LocalTime {
    val parts = timeStr.split(":")
    require(parts.size >= 2) { "Invalid time format: $timeStr" }
    return LocalTime(parts[0].toInt(), parts[1].toInt())
  }
}

/**
 * Custom constraint annotation for validating TimingConstraintRequest.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [TimingConstraintValidator::class])
annotation class ValidTimingConstraint(
  val message: String = "Ongeldige tijdsbeperking",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = []
)

/**
 * Validator for TimingConstraintRequest.
 * Enforces:
 * - If mode is WINDOW or FIXED, windowStart and windowEnd must NOT be null
 * - windowStart must be less than or equal to windowEnd
 * - If mode is DATE_ONLY, time fields are ignored
 */
class TimingConstraintValidator : ConstraintValidator<ValidTimingConstraint, TimingConstraintRequest> {

  override fun isValid(value: TimingConstraintRequest?, context: ConstraintValidatorContext): Boolean {
    if (value == null) {
      return true // Let @NotNull handle null validation
    }

    // Validate date format
    try {
      LocalDate.parse(value.date)
    } catch (e: Exception) {
      context.disableDefaultConstraintViolation()
      context.buildConstraintViolationWithTemplate("Ongeldige datumnotatie: ${value.date}. Verwacht formaat: DD-MM-YYYY")
        .addPropertyNode("date")
        .addConstraintViolation()
      return false
    }

    when (value.mode) {
      TimingMode.DATE_ONLY -> {
        // Time fields are optional/ignored for DATE_ONLY mode
        return true
      }
      TimingMode.WINDOW, TimingMode.FIXED -> {
        // Validate that both time fields are present
        if (value.windowStart == null) {
          context.disableDefaultConstraintViolation()
          context.buildConstraintViolationWithTemplate("Starttijd is verplicht voor modus ${value.mode}")
            .addPropertyNode("windowStart")
            .addConstraintViolation()
          return false
        }

        if (value.windowEnd == null) {
          context.disableDefaultConstraintViolation()
          context.buildConstraintViolationWithTemplate("Eindtijd is verplicht voor modus ${value.mode}")
            .addPropertyNode("windowEnd")
            .addConstraintViolation()
          return false
        }

        // Validate time format and ordering
        try {
          val startTime = parseTime(value.windowStart)
          val endTime = parseTime(value.windowEnd)

          if (startTime > endTime) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate("Starttijd (${value.windowStart}) moet voor of gelijk zijn aan eindtijd (${value.windowEnd})")
              .addConstraintViolation()
            return false
          }
        } catch (e: Exception) {
          context.disableDefaultConstraintViolation()
          context.buildConstraintViolationWithTemplate("Ongeldige tijdnotatie. Verwacht formaat: UU:mm")
            .addConstraintViolation()
          return false
        }

        return true
      }
    }
  }

  private fun parseTime(timeStr: String): LocalTime {
    val parts = timeStr.split(":")
    require(parts.size >= 2) { "Invalid time format: $timeStr" }
    return LocalTime(parts[0].toInt(), parts[1].toInt())
  }
}
