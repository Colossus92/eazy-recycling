package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.config.clock.toDisplayLocalDate
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.TruckDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import org.hibernate.Hibernate


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
    val consignorName: String,
) {

  constructor(transportDto: TransportDto) : this(
    pickupDate = (transportDto.pickupTiming?.toInstant() ?: transportDto.deliveryTiming?.toInstant())?.toDisplayLocalDate().toString(),
    deliveryDate = transportDto.deliveryTiming?.toInstant()?.toDisplayLocalDate()?.toString(),
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
    consignorName = transportDto.consignorParty.name,
  )
}

fun getCityFrom(location: PickupLocationDto) =
  when (val unproxied = Hibernate.unproxy(location)) {
    is PickupLocationDto.DutchAddressDto -> unproxied.city
    is PickupLocationDto.PickupCompanyDto -> unproxied.company.address.city
    is PickupLocationDto.PickupProjectLocationDto -> unproxied.city
    is PickupLocationDto.NoPickupLocationDto -> "n.v.t."
    is PickupLocationDto.ProximityDescriptionDto -> unproxied.city
    else -> throw IllegalStateException("Ongeldige ophaallocatie type: ${unproxied::class.simpleName}")
  }
