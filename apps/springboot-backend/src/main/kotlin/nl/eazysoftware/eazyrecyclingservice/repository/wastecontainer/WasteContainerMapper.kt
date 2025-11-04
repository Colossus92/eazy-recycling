package nl.eazysoftware.eazyrecyclingservice.repository.wastecontainer

import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainer
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationMapper
import org.springframework.stereotype.Component

@Component
class WasteContainerMapper(
  val locationMapper: PickupLocationMapper,
) {

  fun toDomain(dto: WasteContainerDto): WasteContainer {
    return WasteContainer(
      wasteContainerId = WasteContainerId(dto.id),
      location = dto.location?.let { locationMapper.toDomain(it) },
      notes = dto.notes
    )
  }

  fun toDto(domain: WasteContainer): WasteContainerDto {
    return WasteContainerDto(
      id = domain.wasteContainerId.id,
      notes = domain.notes,
      location = domain.location?.let { locationMapper.toDto(it) }
    )
  }

}
