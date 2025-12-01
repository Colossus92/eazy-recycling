package nl.eazysoftware.eazyrecyclingservice.repository.wastecontainer

import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainer
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationMapper
import org.springframework.stereotype.Component
import kotlin.time.toKotlinInstant

@Component
class WasteContainerMapper(
  val locationMapper: PickupLocationMapper,
) {

  fun toDomain(dto: WasteContainerDto): WasteContainer {
    return WasteContainer(
      wasteContainerId = WasteContainerId(dto.id),
      location = dto.location?.let { locationMapper.toDomain(it) },
      notes = dto.notes,
      createdAt = dto.createdAt?.toKotlinInstant(),
      createdBy = dto.createdBy,
      updatedAt = dto.updatedAt?.toKotlinInstant(),
      updatedBy = dto.updatedBy,
    )
  }

  fun toDto(domain: WasteContainer): WasteContainerDto {
    return WasteContainerDto(
      id = domain.wasteContainerId.id,
      notes = domain.notes,
      location = domain.location?.let { locationMapper.toDto(it) },
    )
  }

}
