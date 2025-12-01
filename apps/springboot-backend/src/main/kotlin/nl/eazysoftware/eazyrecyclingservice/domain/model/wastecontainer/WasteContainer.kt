package nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import kotlin.time.Instant

data class WasteContainer(
  val wasteContainerId: WasteContainerId,
  var location: Location?,
  val notes: String?,
  val createdAt: Instant? = null,
  val createdBy: String? = null,
  val updatedAt: Instant? = null,
  val updatedBy: String? = null,
)

data class WasteContainerId(
  val id: String,
) {
  init {
      require(!id.isBlank()) { "Een containerkenmerk moet een waarde hebben" }
  }
}
