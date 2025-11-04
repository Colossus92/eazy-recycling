package nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location

data class WasteContainer(
  val wasteContainerId: WasteContainerId,
  var location: Location?,
  val notes: String?,
)

data class WasteContainerId(
  val id: String,
) {
  init {
      require(!id.isBlank()) { "Een containerkenmerk moet een waarde hebben" }
  }
}
