package nl.eazysoftware.eazyrecyclingservice.repository.entity.container

import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import java.util.UUID

data class Container(
    val uuid: UUID,
    val id: String,
    val location: ContainerLocation,
    val notes: String,
) {
    data class ContainerLocation(
        val companyId: UUID?,
        val companyName: String?,
        val address: AddressDto?,
    ) {

    }
}