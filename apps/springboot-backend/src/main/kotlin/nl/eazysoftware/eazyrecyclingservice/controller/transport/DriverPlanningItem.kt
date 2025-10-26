package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.application.query.PickupLocationView
import nl.eazysoftware.eazyrecyclingservice.repository.address.toPickupLocationView
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class DriverPlanningItem(
    val id: UUID?,
    val displayNumber: String?,
    val pickupDateTime: LocalDateTime,
    val deliveryDateTime: LocalDateTime?,
    val pickupLocation: PickupLocationView,
    val deliveryLocation: PickupLocationView,
    val containerId: String?,
    val status: TransportDto.Status,
) {
    constructor(transport: TransportDto) : this(
        transport.id,
        transport.displayNumber,
        transport.pickupDateTime,
        transport.deliveryDateTime,
        transport.pickupLocation.toPickupLocationView(),
        transport.deliveryLocation.toPickupLocationView(),
        transport.wasteContainer?.id ?: "-",
        transport.getStatus(),
    )
}

typealias DriverPlanning = Map<LocalDate, Map<String, List<DriverPlanningItem>>>
