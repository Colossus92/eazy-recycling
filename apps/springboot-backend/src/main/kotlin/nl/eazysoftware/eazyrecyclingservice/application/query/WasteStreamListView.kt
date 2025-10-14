package nl.eazysoftware.eazyrecyclingservice.application.query

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime

data class WasteStreamListView(
    val wasteStreamNumber: String,
    val wasteName: String,
    val euralCode: String,
    val processingMethodCode: String,
    val consignorPartyName: String,
    val pickupLocation: String,
    val deliveryLocation: String,
    val status: String,
    val lastActivityAt: LocalDateTime,
)
