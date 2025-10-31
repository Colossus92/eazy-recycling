package nl.eazysoftware.eazyrecyclingservice.controller.transport.containertransport

import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.ContainerOperation
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import java.time.LocalDateTime
import java.util.*

class ContainerTransportRequest(
    val consignorPartyId: UUID,
    val pickupDateTime: LocalDateTime,
    val deliveryDateTime: LocalDateTime?,
    val transportType: TransportType,
    val containerOperation: ContainerOperation,
    val driverId: UUID?,
    val carrierPartyId: UUID,
    val pickupCompanyId: UUID?,
    val pickupProjectLocationId: UUID? = null,
    val pickupStreet: String?,
    val pickupBuildingNumber: String?,
    val pickupBuildingNumberAddition: String? = null,
    val pickupPostalCode: String?,
    val pickupCity: String?,
    val pickupDescription: String? = null,
    val deliveryCompanyId: UUID?,
    val deliveryProjectLocationId: UUID? = null,
    val deliveryStreet: String,
    val deliveryBuildingNumber: String,
    val deliveryBuildingNumberAddition: String? = null,
    val deliveryPostalCode: String,
    val deliveryCity: String,
    val deliveryDescription: String? = null,
    val truckId: String?,
    val containerId: String?,
    val note: String,
)
