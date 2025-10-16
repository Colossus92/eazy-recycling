package nl.eazysoftware.eazyrecyclingservice.domain.model

import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import java.util.UUID

data class WasteContainer(
  val wasteContainerId: WasteContainerId,
  val id: String,
  val location: ContainerLocation?,
  val notes: String?,
) {

  data class ContainerLocation(
    val companyId: UUID?,
    val companyName: String?,
    val address: AddressDto?,
  ) {

  }
}

data class WasteContainerId(
  val uuid: UUID,
)
