package nl.eazysoftware.eazyrecyclingservice.application.query

import kotlinx.datetime.Instant
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
  val pickupDateTime: Instant,
  val deliveryLocation: PickupLocationView,
  val deliveryDateTime: Instant,
  val transportType: String,
  val status: String,
  val truck: TruckView?,
  val driver: DriverView?,
  val note: String,
  val transportHours: Double?,
  val sequenceNumber: Int,
  val updatedAt: Instant?,
  val wasteContainer: WasteContainerView?
)

data class WasteContainerView(
  val uuid: UUID,
  val containerNumber: String,
)

data class TruckView(
  val licensePlate: String
)

data class DriverView(
  val id: UUID,
  val name: String,
)
