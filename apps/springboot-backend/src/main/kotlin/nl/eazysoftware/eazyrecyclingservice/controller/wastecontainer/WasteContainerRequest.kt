package nl.eazysoftware.eazyrecyclingservice.controller.wastecontainer

import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.PickupLocationRequest

data class WasteContainerRequest(
    val id: String,
    val location: PickupLocationRequest?,
    val notes: String?,
)
