package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.domain.weightticket.WeightTicketStatus

data class WeightTicketListView(
    val id: Long,
    val consignorPartyName: String,
    val note: String?,
    val status: WeightTicketStatus,
)
