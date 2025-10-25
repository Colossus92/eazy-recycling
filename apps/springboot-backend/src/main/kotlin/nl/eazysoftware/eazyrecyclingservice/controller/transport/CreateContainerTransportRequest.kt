package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.ContainerOperation
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import java.time.LocalDateTime
import java.util.*

open class CreateContainerTransportRequest(
  open val consignorPartyId: UUID,
  open val pickupDateTime: LocalDateTime,
  open val deliveryDateTime: LocalDateTime?,
  open val transportType: TransportType,
  open val containerOperation: ContainerOperation,
  open val driverId: UUID?,
  open val carrierPartyId: UUID,
  open val pickupCompanyId: UUID?,
  open val pickupProjectLocationId: UUID? = null,
  open val pickupStreet: String?,
  open val pickupBuildingNumber: String?,
  open val pickupBuildingNumberAddition: String? = null,
  open val pickupPostalCode: String?,
  open val pickupCity: String?,
  open val pickupDescription: String? = null,
  open val deliveryCompanyId: UUID?,
  open val deliveryProjectLocationId: UUID? = null,
  open val deliveryStreet: String,
  open val deliveryBuildingNumber: String,
  open val deliveryBuildingNumberAddition: String? = null,
  open val deliveryPostalCode: String,
  open val deliveryCity: String,
  open val deliveryDescription: String? = null,
  open val truckId: String?,
  open val containerId: UUID?,
  open val note: String,
)
