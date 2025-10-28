package nl.eazysoftware.eazyrecyclingservice.application.query

import kotlinx.datetime.Instant
import nl.eazysoftware.eazyrecyclingservice.controller.wastecontainer.WasteContainerView
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.ContainerOperation
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportStatus
import java.util.*

/**
 * View model for container transport details.
 * Flat structure for easy consumption by the frontend.
 */
data class TransportDetailView(
  val id: UUID,
  val displayNumber: String?,
  val consignorParty: CompanyView,
  val carrierParty: CompanyView,
  val pickupLocation: PickupLocationView,
  val pickupDateTime: String,
  val deliveryLocation: PickupLocationView,
  val deliveryDateTime: String?,
  val transportType: String,
  val status: TransportStatus,
  val truck: TruckView?,
  val driver: DriverView?,
  val note: String,
  val transportHours: Double?,
  val sequenceNumber: Int,
  val updatedAt: Instant?,
  val wasteContainer: WasteContainerView?,
  val containerOperation: ContainerOperation?,

  // Waste transport specific details
  val goodsItem: GoodsItemView? = null,
  val consigneeParty: CompanyView? = null,
  val pickupParty: CompanyView? = null,
)

data class TruckView(
  val licensePlate: String,
  val brand: String,
  val model: String,
)

data class DriverView(
  val id: UUID,
  val name: String,
)

data class GoodsItemView(
  val wasteStreamNumber: String,
  val name: String,
  val netNetWeight: Double,
  val unit: String,
  val quantity: Int,
  val euralCode: String,
  val processingMethodCode: String,
  val consignorClassification: Int = 1,
)

