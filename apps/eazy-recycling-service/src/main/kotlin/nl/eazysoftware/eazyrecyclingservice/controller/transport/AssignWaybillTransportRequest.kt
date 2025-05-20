package nl.eazysoftware.eazyrecyclingservice.controller.transport

import java.util.UUID

data class AssignWaybillTransportRequest(
    val licensePlate: String,
    val waybillId: UUID,
    val driverId: UUID,
)