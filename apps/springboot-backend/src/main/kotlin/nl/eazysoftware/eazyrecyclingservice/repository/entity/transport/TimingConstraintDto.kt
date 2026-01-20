package nl.eazysoftware.eazyrecyclingservice.repository.entity.transport

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TimingConstraint
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TimingMode
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant

/**
 * JPA embeddable for persisting TimingConstraint values.
 * Used for both pickup and delivery timing constraints in TransportDto.
 */
@Embeddable
data class TimingConstraintDto(
  @Column(nullable = false)
  val date: java.time.LocalDate,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val mode: TimingMode,

  @Column(nullable = true)
  val windowStart: java.time.LocalTime?,

  @Column(nullable = true)
  val windowEnd: java.time.LocalTime?
) {
  /**
   * Converts this DTO to a domain TimingConstraint.
   */
  fun toDomain(): TimingConstraint {
    return TimingConstraint(
      date = LocalDate(date.year, date.monthValue, date.dayOfMonth),
      mode = mode,
      windowStart = windowStart?.let { LocalTime(it.hour, it.minute) },
      windowEnd = windowEnd?.let { LocalTime(it.hour, it.minute) }
    )
  }

  /**
   * Converts this timing constraint to an Instant for backward compatibility.
   * Uses the windowStart time if available, otherwise start of day.
   */
  fun toInstant(): java.time.Instant {
    return toDomain().toInstant().toJavaInstant()
  }

  companion object {
    /**
     * Creates a TimingConstraintDto from a domain TimingConstraint.
     */
    fun fromDomain(constraint: TimingConstraint): TimingConstraintDto {
      return TimingConstraintDto(
        date = java.time.LocalDate.of(
          constraint.date.year,
          constraint.date.monthNumber,
          constraint.date.dayOfMonth
        ),
        mode = constraint.mode,
        windowStart = constraint.windowStart?.let {
          java.time.LocalTime.of(it.hour, it.minute)
        },
        windowEnd = constraint.windowEnd?.let {
          java.time.LocalTime.of(it.hour, it.minute)
        }
      )
    }

    /**
     * Creates a TimingConstraintDto from a legacy Instant value.
     * Converts to a FIXED constraint at the specified time.
     */
    fun fromInstant(instant: Instant): TimingConstraintDto {
      val constraint = TimingConstraint.fromInstant(instant)
      return fromDomain(constraint)
    }

    /**
     * Creates a TimingConstraintDto from a legacy java.time.Instant value.
     */
    fun fromJavaInstant(instant: java.time.Instant): TimingConstraintDto {
      return fromInstant(kotlin.time.Instant.fromEpochMilliseconds(instant.toEpochMilli()))
    }
  }
}
