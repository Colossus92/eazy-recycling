package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.ContainerOperation
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportType
import java.time.LocalDateTime
import java.util.*

open class CreateContainerTransportRequest(
    open val consignorPartyId: UUID,
    open val pickupDateTime: LocalDateTime,
    open val deliveryDateTime: LocalDateTime?,
    open val transportType: TransportType,
    open val containerOperation: ContainerOperation,
    open val driverId: UUID?,
    open val carrierPartyId: UUID,
    open val pickupCompanyId: UUID?,
    open val pickupCompanyBranchId: UUID? = null,
    open val pickupStreet: String,
    open val pickupBuildingNumber: String,
    open val pickupPostalCode: String,
    open val pickupCity: String,
    open val deliveryCompanyId: UUID?,
    open val deliveryCompanyBranchId: UUID? = null,
    open val deliveryStreet: String,
    open val deliveryBuildingNumber: String,
    open val deliveryPostalCode: String,
    open val deliveryCity: String,
    open val truckId: String?,
    open val containerId: UUID?,
    open val note: String,
)
