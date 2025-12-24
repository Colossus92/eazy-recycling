package nl.eazysoftware.eazyrecyclingservice.domain.model.declaration

import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import java.math.BigDecimal
import kotlin.time.Instant

/**
 * Represents a weight ticket line that has not yet been declared.
 */
data class UndeclaredWeightTicketLine(
  val weightTicketId: WeightTicketId,
  val weightTicketLineIndex: Int,
  val wasteStreamNumber: WasteStreamNumber,
  val weightValue: BigDecimal,
  val weightedAt: Instant,
)
