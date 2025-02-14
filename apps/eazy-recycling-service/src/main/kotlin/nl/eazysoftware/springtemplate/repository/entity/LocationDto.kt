package nl.eazysoftware.springtemplate.repository.entity

import jakarta.persistence.*

@Entity
data class LocationDto(
    @Id
    val id: String,
    val description: String,
    val locationTypeCode: String,
    @Embedded
    val address: AddressDto,
) {
}