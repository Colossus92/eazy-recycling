package nl.eazysoftware.eazyrecyclingservice.controller.transport

import kotlinx.datetime.DayOfWeek
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto


data class PlanningView(
    val dates: List<DateInfo>,
    val transports: List<TransportsView>,
) {

}

data class DateInfo(
    val date: String,
    val dayOfWeek: DayOfWeek,
){
}

data class TransportsView(
    val truck: String,
    val transports: Map<String, List<TransportView>>
) {

}

data class TransportView(
    val date: String,
    val id: String,
    val truckLicensePlate: String
) {

    constructor(transportDto: TransportDto): this(
        date = transportDto.pickupDateTime.toLocalDate().toString(),
        id = transportDto.id.toString(),
        truckLicensePlate =  transportDto.truck?.licensePlate ?: "NOT_ASSIGNED"
    )
}