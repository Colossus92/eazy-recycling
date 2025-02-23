package nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill

import jakarta.persistence.Embeddable
import lombok.NoArgsConstructor

@Embeddable
@NoArgsConstructor
data class AddressDto(
    var streetName: String?,
    var buildingName: String? = null,
    var buildingNumber: String?,
    var city: String?,
    var postalCode: String?,
    var country: String?


)