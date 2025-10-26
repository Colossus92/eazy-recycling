package nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill

import jakarta.persistence.Embeddable

@Embeddable
data class AddressDto(
    var streetName: String?,
    // TODO rename to buildingnumberaddition
    var buildingName: String? = null,
    var buildingNumber: String,
    var city: String?,
    var postalCode: String,
    var country: String?
)
