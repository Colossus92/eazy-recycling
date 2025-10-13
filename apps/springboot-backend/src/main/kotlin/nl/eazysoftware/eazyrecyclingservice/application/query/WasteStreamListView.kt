package nl.eazysoftware.eazyrecyclingservice.application.query

data class WasteStreamListView(
    val wasteStreamNumber: String,
    val wasteName: String,
    val euralCode: String,
    val processingMethodCode: String,
    val consignorPartyChamberOfCommerceId: String?,
    val consignorPartyName: String,
    val pickupLocationStreetName: String?,
    val pickupLocationPostalCode: String?,
    val pickupLocationNumber: String?,
    val pickupLocationCity: String,
    val deliveryLocationStreetName: String,
    val deliveryLocationPostalCode: String?,
    val deliveryLocationNumber: String?,
    val deliveryLocationCity: String,
)
