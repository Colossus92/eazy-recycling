package nl.eazysoftware.eazyrecyclingservice.controller.wastecontainer

import nl.eazysoftware.eazyrecyclingservice.application.query.PickupLocationView

data class WasteContainerView(
  val id: String,
  val location: PickupLocationView?,
  val notes: String?,
)
