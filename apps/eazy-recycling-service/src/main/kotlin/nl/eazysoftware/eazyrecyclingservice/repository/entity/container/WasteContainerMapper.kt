package nl.eazysoftware.eazyrecyclingservice.repository.entity.container

import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainer
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import org.springframework.stereotype.Component

@Component
class WasteContainerMapper {


    fun toDomain(dto: WasteContainerDto): WasteContainer {
        return WasteContainer(
            uuid = dto.uuid!!,
            id = dto.id,
            location = WasteContainer.ContainerLocation(
                companyId = dto.company?.id,
                companyName = dto.company?.name,
                address = toAddress(dto)
            ),
            notes = dto.notes
        )
    }

    private fun toAddress(dto: WasteContainerDto): AddressDto {
        return dto.company?.address
            ?: dto.address
            ?: throw IllegalStateException("No address found")
    }
}