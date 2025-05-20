package nl.eazysoftware.eazyrecyclingservice.controller.transport

import java.util.UUID

data class CreateWaybillTransportRequest(
    val licensePlate: String,
    val waybillId: UUID,
    val driverId: UUID,
)