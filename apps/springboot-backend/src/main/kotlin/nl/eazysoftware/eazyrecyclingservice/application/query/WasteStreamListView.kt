package nl.eazysoftware.eazyrecyclingservice.application.query

import kotlinx.datetime.LocalDateTime
import java.util.*

data class WasteStreamListView(
    val wasteStreamNumber: String,
    val wasteName: String,
    val euralCode: String,
    val processingMethodCode: String,
    val consignorPartyName: String,
    val consignorPartyId: UUID,
    val pickupLocation: String,
    val deliveryLocation: String,
    val status: String,
    val lastActivityAt: LocalDateTime,
    val isEditable: Boolean,
)
