package nl.eazysoftware.eazyrecyclingservice.controller.transport.wastetransport

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.ContainerOperation
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import java.time.LocalDateTime
import java.util.*

data class WasteTransportRequest(
  val pickupDateTime: LocalDateTime,
  val deliveryDateTime: LocalDateTime?,
  val containerOperation: ContainerOperation,
  val transportType: TransportType,
  val driverId: UUID?,
  val carrierPartyId: UUID,
  val truckId: String?,
  val containerId: String?,
  val note: String,
  val goods: List<GoodsRequest>,
)

data class GoodsRequest(
  val wasteStreamNumber: String,
  @field:Min(0)
  val weight: Double,
  @field:NotBlank
  val unit: String,
  @field:Min(0)
  val quantity: Int,
)
