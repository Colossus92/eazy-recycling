package nl.eazysoftware.eazyrecyclingservice.application.query.mappers

import nl.eazysoftware.eazyrecyclingservice.controller.wastecontainer.WasteContainerView
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainer
import org.springframework.stereotype.Component
import kotlin.time.toJavaInstant

@Component
class WasteContainerViewMapper(
  private val pickupLocationViewMapper: PickupLocationViewMapper,
) {

  fun map(container: WasteContainer): WasteContainerView {
    return WasteContainerView(
      id = container.wasteContainerId.id,
      location = container.location?.let { pickupLocationViewMapper.mapLocation(it) },
      notes = container.notes,
      createdAt = container.createdAt?.toJavaInstant(),
      createdByName = container.createdBy,
      updatedAt = container.updatedAt?.toJavaInstant(),
      updatedByName = container.updatedBy,
    )
  }
}
