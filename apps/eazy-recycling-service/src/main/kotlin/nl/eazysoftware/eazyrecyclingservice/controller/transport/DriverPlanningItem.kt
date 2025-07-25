package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.LocationDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DriverPlanningItem(
    val id: UUID?,
    val displayNumber: String?,
    val pickupDateTime: LocalDateTime,
    val deliveryDateTime: LocalDateTime?,
    val pickupLocation: LocationDto,
    val deliveryLocation: LocationDto,
    val containerId: String?,
    val status: TransportDto.Status,
) {
    constructor(transport: TransportDto) : this(
        transport.id,
        transport.displayNumber,
        transport.pickupDateTime,
        transport.deliveryDateTime,
        transport.pickupLocation,
        transport.deliveryLocation,
        transport.wasteContainer?.id ?: "-",
        transport.getStatus(),
    )
}

typealias DriverPlanning = Map<LocalDate, Map<String, List<DriverPlanningItem>>>