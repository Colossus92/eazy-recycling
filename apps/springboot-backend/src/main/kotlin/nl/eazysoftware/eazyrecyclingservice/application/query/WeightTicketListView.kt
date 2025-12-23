package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketStatus
import java.time.Instant

data class WeightTicketListView(
    val id: Long,
    val consignorPartyName: String,
    val totalWeight: Double?,
    val weighingDate: Instant?,
    val note: String?,
    val status: WeightTicketStatus,
)
