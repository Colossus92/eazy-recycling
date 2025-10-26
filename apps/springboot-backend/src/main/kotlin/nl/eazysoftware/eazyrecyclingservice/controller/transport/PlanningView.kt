package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto


data class PlanningView(
  val dates: List<String>,
  val transports: List<TransportsView>,
)

data class TransportsView(
  val truck: String,
  val transports: Map<String, List<TransportView>>
)

data class TransportView(
  val pickupDate: String,
  val deliveryDate: String?,
  val id: String,
  val truck: Truck?,
  val originCity: String?,
  val destinationCity: String?,
  val driver: ProfileDto?,
  val status: TransportDto.Status,
  val displayNumber: String?,
  val containerId: String?,
  val transportType: TransportType,
  val sequenceNumber: Int,
) {

  constructor(transportDto: TransportDto) : this(
    pickupDate = transportDto.pickupDateTime.toLocalDate().toString(),
    deliveryDate = transportDto.deliveryDateTime?.toLocalDate()?.toString()
      .takeIf { transportDto.deliveryDateTime != null },
    id = transportDto.id.toString(),
    truck = transportDto.truck,
    originCity = getCityFrom(transportDto.pickupLocation),
    destinationCity = getCityFrom(transportDto.deliveryLocation),
    driver = transportDto.driver,
    status = transportDto.getStatus(),
    displayNumber = transportDto.displayNumber,
    containerId = transportDto.wasteContainer?.id,
    transportType = transportDto.transportType,
    sequenceNumber = transportDto.sequenceNumber,
  )
}

fun getCityFrom(location: PickupLocationDto) =
  when (location) {
    is PickupLocationDto.DutchAddressDto -> location.city
    is PickupLocationDto.PickupCompanyDto -> location.company.address.city ?: "niet bekend"
    is PickupLocationDto.PickupProjectLocationDto -> location.city
    is PickupLocationDto.NoPickupLocationDto -> "n.v.t."
    is PickupLocationDto.ProximityDescriptionDto -> location.city
    else -> throw IllegalStateException("Ongeldige ophaallocatie type: ${location::class.simpleName}")
  }
