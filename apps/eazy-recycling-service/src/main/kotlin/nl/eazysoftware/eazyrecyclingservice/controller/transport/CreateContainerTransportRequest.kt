package nl.eazysoftware.eazyrecyclingservice.controller

import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportType
import java.time.LocalDateTime
import java.util.*

data class CreateContainerTransportRequest(
    val consignorPartyId: UUID,
    val pickupDateTime: LocalDateTime,
    val deliveryDateTime: LocalDateTime,
    val transportType: TransportType,
    val driverId: UUID?,
    val carrierPartyId: UUID,
    val pickupCompanyId: UUID?,
    val pickupStreet: String,
    val pickupHouseNumber: String,
    val pickupPostalCode: String,
    val pickupCity: String,
    val deliveryCompanyId: UUID?,
    val deliveryStreet: String,
    val deliveryHouseNumber: String,
    val deliveryPostalCode: String,
    val deliveryCity: String,
    val truckId: String?,
    val containerId: UUID?,
)

data class AddressRequest(
    val streetName: String,
    val buildingNumber: String,
    val postalCode: String,
    val city: String,
    val country: String = "Nederland"
)