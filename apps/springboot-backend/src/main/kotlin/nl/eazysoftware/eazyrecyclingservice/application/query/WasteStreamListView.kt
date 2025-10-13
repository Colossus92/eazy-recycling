package nl.eazysoftware.eazyrecyclingservice.application.query

data class WasteStreamListView(
    val wasteStreamNumber: String,
    val wasteName: String,
    val euralCode: String,
    val processingMethodCode: String,
    val consignorPartyName: String,
    val pickupLocation: String,
    val deliveryLocation: String,
)
