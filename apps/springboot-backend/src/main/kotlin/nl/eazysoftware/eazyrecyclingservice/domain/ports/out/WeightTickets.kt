package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.domain.model.declaration.UndeclaredWeightTicketLine
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicket
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import kotlin.time.Instant

interface WeightTickets {
  fun nextId(): WeightTicketId
  fun save(aggregate: WeightTicket): WeightTicket
  fun findById(id: WeightTicketId): WeightTicket?

  /**
   * Marks specific weight ticket lines for the given waste stream number and weight ticket IDs as declared.
   * Updates the declared_weight to the current weight_value and sets last_declared_at to the given timestamp.
   *
   * @param wasteStreamNumber The waste stream number to mark as declared
   * @param weightTicketIds The specific weight ticket IDs whose lines should be marked as declared
   * @param declaredAt The timestamp when the declaration was made
   * @return The number of lines updated
   */
  fun markLinesAsDeclared(
    wasteStreamNumber: WasteStreamNumber,
    weightTicketIds: List<Long>,
    declaredAt: Instant
  ): Int

  /**
   * Finds all weight ticket lines that have not been fully declared yet.
   * Only includes tickets with status COMPLETED or INVOICED from months that have passed their declaration deadline.
   *
   * A line is considered undeclared if:
   * - declared_weight is NULL, OR
   * - declared_weight != weight_value (partially declared)
   *
   * @param cutoffDate The cutoff date - only weight tickets with weightedAt before this date are included
   * @return List of undeclared weight ticket lines
   */
  fun findUndeclaredLines(cutoffDate: YearMonth): List<UndeclaredWeightTicketLine>
}
