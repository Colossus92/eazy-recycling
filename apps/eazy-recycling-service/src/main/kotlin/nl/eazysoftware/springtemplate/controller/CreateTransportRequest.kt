package nl.eazysoftware.springtemplate.controller

import java.util.UUID

data class CreateTransportRequest(
    val licensePlate: String,
    val waybillId: UUID,
    val driverId: UUID,
)