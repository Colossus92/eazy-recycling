package nl.eazysoftware.eazyrecyclingservice.application.query

import kotlinx.datetime.Instant

data class WasteStreamListView(
    val wasteStreamNumber: String,
    val wasteName: String,
    val euralCode: String,
    val processingMethodCode: String,
    val consignorPartyName: String,
    val pickupLocation: String,
    val deliveryLocation: String,
    val status: String,
    val lastActivityAt: Instant,
)
