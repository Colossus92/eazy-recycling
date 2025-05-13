package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.repository.entity.driver.Driver
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck


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
    val deliveryDate: String,
    val id: String,
    val truck: Truck?,
    val originCity: String?,
    val destinationCity: String?,
    val driver: Driver?,
    val status: TransportDto.Status,
    val displayNumber: String?,
) {

    constructor(transportDto: TransportDto): this(
        pickupDate = transportDto.pickupDateTime.toLocalDate().toString(),
        deliveryDate = transportDto.deliveryDateTime.toLocalDate().toString(),
        id = transportDto.id.toString(),
        truck =  transportDto.truck,
        originCity = transportDto.pickupLocation.address.city,
        destinationCity = transportDto.deliveryLocation.address.city,
        driver = transportDto.driver,
        status = transportDto.getStatus(),
        displayNumber = transportDto.displayNumber
    )
}