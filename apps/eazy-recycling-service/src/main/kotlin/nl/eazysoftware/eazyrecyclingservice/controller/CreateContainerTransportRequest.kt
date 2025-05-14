package nl.eazysoftware.eazyrecyclingservice.controller

import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportType
import java.time.LocalDateTime
import java.util.*

data class CreateContainerTransportRequest(
    val id: String?,
    val consignorPartyId: UUID,
    val pickupDateTime: LocalDateTime,
    val deliveryDateTime: LocalDateTime,
    val containerType: String?,
    val typeOfTransport: TransportType,
    val driverId: UUID?,
    val carrierPartyId: UUID,
    val pickupPartyId: String?,
    val pickupStreet: String,
    val pickupHouseNumber: String,
    val pickupPostalCode: String,
    val pickupCity: String,
    val consigneePartyId: String?,
    val deliveryPartyId: String,
    val deliveryStreet: String,
    val deliveryHouseNumber: String,
    val deliveryPostalCode: String,
    val deliveryCity: String,
    val truckId: String?,
    val driver: UUID?,
    val containerId: UUID?,
)

data class AddressRequest(
    val streetName: String,
    val buildingNumber: String,
    val postalCode: String,
    val city: String,
    val country: String = "Nederland"
)