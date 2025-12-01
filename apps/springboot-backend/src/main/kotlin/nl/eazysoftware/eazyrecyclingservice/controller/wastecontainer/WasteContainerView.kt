package nl.eazysoftware.eazyrecyclingservice.controller.wastecontainer

import nl.eazysoftware.eazyrecyclingservice.application.query.PickupLocationView
import java.time.Instant

data class WasteContainerView(
  val id: String,
  val location: PickupLocationView?,
  val notes: String?,
  val createdAt: Instant? = null,
  val createdByName: String? = null,
  val updatedAt: Instant? = null,
  val updatedByName: String? = null,
)
