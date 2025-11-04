package nl.eazysoftware.eazyrecyclingservice.controller.transport.containertransport

import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.PickupLocationRequest
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.ContainerOperation
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import java.time.LocalDateTime
import java.util.*

class ContainerTransportRequest(
  val consignorPartyId: UUID,
  val pickupDateTime: LocalDateTime,
  val deliveryDateTime: LocalDateTime?,
  val transportType: TransportType,
  val containerOperation: ContainerOperation,
  val driverId: UUID?,
  val carrierPartyId: UUID,
  val pickupLocation: PickupLocationRequest,
  val deliveryLocation: PickupLocationRequest,
  val truckId: String?,
  val containerId: String?,
  val note: String,
)
