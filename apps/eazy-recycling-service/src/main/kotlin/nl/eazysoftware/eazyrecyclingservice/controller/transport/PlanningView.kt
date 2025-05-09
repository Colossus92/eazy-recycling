package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.repository.entity.driver.Driver
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto


data class PlanningView(
    val dates: List<String>,
    val transports: List<TransportsView>,
)

data class TransportsView(
    val truck: String,
    val transports: Map<String, List<TransportView>>
)

data class TransportView(
    val date: String,
    val id: String,
    val truckLicensePlate: String,
    val originCity: String?,
    val destinationCity: String?,
    val driver: Driver?
) {

    constructor(transportDto: TransportDto): this(
        date = transportDto.pickupDateTime.toLocalDate().toString(),
        id = transportDto.id.toString(),
        truckLicensePlate =  transportDto.truck?.licensePlate ?: "NOT_ASSIGNED",
        originCity = transportDto.pickupLocation.address.city,
        destinationCity = transportDto.deliveryLocation.address.city,
        driver = transportDto.driver,
    )
}