package nl.eazysoftware.eazyrecyclingservice.adapters.out.web

import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TimingConstraint
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TimingMode

/**
 * View DTO for timing constraint data sent to the frontend.
 */
data class TimingConstraintView(
  val date: String,
  val mode: TimingMode,
  val windowStart: String?,
  val windowEnd: String?
) {
  companion object {
    /**
     * Creates a TimingConstraintView from a domain TimingConstraint.
     */
    fun fromDomain(constraint: TimingConstraint): TimingConstraintView {
      return TimingConstraintView(
        date = constraint.date.toString(),
        mode = constraint.mode,
        windowStart = constraint.windowStart?.let { formatTime(it) },
        windowEnd = constraint.windowEnd?.let { formatTime(it) }
      )
    }

    private fun formatTime(time: kotlinx.datetime.LocalTime): String {
      return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
    }
  }
}
