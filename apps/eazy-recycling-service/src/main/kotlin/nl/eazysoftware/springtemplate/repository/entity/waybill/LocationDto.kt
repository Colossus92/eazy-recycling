package nl.eazysoftware.springtemplate.repository.entity.waybill

import jakarta.persistence.*

@Entity
@Table(name = "locations")
data class LocationDto(
    @Id
    val id: String,
    val description: String,
    val locationTypeCode: String,
    @Embedded
    val address: AddressDto,
)