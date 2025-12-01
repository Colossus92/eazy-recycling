package nl.eazysoftware.eazyrecyclingservice.application.query

import kotlinx.datetime.LocalDateTime
import java.math.BigDecimal

/**
 * View representing a weight ticket that contains a line for a specific waste stream.
 */
data class WeightTicketsByWasteStreamView(
    val weightTicketNumber: Long,
    val weightedAt: LocalDateTime?,
    val amount: BigDecimal,
    val createdBy: String?
)
