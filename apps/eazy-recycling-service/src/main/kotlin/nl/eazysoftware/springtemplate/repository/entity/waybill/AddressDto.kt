package nl.eazysoftware.springtemplate.repository.entity.waybill

import jakarta.persistence.Embeddable
import jakarta.persistence.Table
import lombok.NoArgsConstructor

@Embeddable
@NoArgsConstructor
@Table(name = "addresses")
data class AddressDto(
    var streetName: String?,
    var buildingName: String?,
    var buildingNumber: String?,
    var city: String?,
    var postalCode: String?,
    var country: String?


)