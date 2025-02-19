package nl.eazysoftware.springtemplate.controller

import java.util.UUID

data class CreateWaybillTransportRequest(
    val licensePlate: String,
    val waybillId: UUID,
    val driverId: UUID,
)