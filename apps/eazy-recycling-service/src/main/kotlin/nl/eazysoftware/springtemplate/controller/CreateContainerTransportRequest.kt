package nl.eazysoftware.springtemplate.controller

import nl.eazysoftware.springtemplate.repository.entity.transport.TransportType
import java.time.LocalDateTime
import java.util.UUID

data class CreateContainerTransportRequest(
    val id: String,
    val customerId: UUID,
    val pickupDateTime: LocalDateTime,
    val originAddress: AddressRequest,
    val deliveryDateTime: LocalDateTime,
    val destinationAddress: AddressRequest,
    val containerType: String,
    val transportType: TransportType,
    val licensePlate: String,
    val driverId: UUID,
)

data class AddressRequest(
    val streetName: String,
    val buildingNumber: String,
    val postalCode: String,
    val city: String,
    val country: String = "Nederland"
)