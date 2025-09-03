package nl.eazysoftware.eazyrecyclingservice.controller.wastecontainer

import nl.eazysoftware.eazyrecyclingservice.controller.request.AddressRequest
import java.util.*

data class CreateContainerRequest(
    val id: String,
    val location: ContainerLocation?,
    val notes: String?,
) {
    data class ContainerLocation(
        val companyId: UUID?,
        val companyName: String?,
        val address: AddressRequest?,
    ) {

    }
}