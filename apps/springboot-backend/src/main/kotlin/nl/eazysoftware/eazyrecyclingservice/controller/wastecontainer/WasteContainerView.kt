package nl.eazysoftware.eazyrecyclingservice.controller.wastecontainer

import nl.eazysoftware.eazyrecyclingservice.application.query.AddressView
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainer
import java.util.*

data class WasteContainerView(
  val id: String,
  val location: ContainerLocationView,
  val notes: String?,
)

data class ContainerLocationView(
  val companyId: UUID?,
  val companyName: String?,
  val addressView: AddressView?,
)

fun WasteContainer.toView() = WasteContainerView(
  id = this.wasteContainerId.id,
  location = ContainerLocationView(
    companyId = this.location?.companyId,
    companyName = this.location?.companyName,
    addressView = this.location?.address?.let {
      AddressView(
        street = it.streetName ?: "",
        houseNumber = it.buildingNumber,
        houseNumberAddition = it.buildingName,
        postalCode = it.postalCode,
        city = it.city ?: "",
        country = it.country ?: "",
      )
    }
  ),
  notes = this.notes,
)
