package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.TruckDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import org.hibernate.Hibernate
import java.time.ZoneId


data class PlanningView(
  val dates: List<String>,
  val transports: List<PlanningTransportsView>,
)

data class PlanningTransportsView(
  val truck: String,
  val transports: Map<String, List<PlanningTransportView>>
)

data class PlanningTransportView(
    val pickupDate: String,
    val deliveryDate: String?,
    val id: String,
    val truck: TruckDto?,
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
    pickupDate = transportDto.pickupDateTime.atZone(ZoneId.of("Europe/Amsterdam")).toLocalDate().toString(),
    deliveryDate = transportDto.deliveryDateTime?.atZone(ZoneId.of("Europe/Amsterdam"))?.toLocalDate()?.toString()
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
  when (val unproxied = Hibernate.unproxy(location)) {
    is PickupLocationDto.DutchAddressDto -> unproxied.city
    is PickupLocationDto.PickupCompanyDto -> unproxied.company.address.city ?: "niet bekend"
    is PickupLocationDto.PickupProjectLocationDto -> unproxied.city
    is PickupLocationDto.NoPickupLocationDto -> "n.v.t."
    is PickupLocationDto.ProximityDescriptionDto -> unproxied.city
    else -> throw IllegalStateException("Ongeldige ophaallocatie type: ${unproxied::class.simpleName}")
  }
