package nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill

import jakarta.persistence.*

@Entity
@Table(name = "locations")
data class LocationDto(
    @Id
    val id: String,
    val description: String? = null,
    val locationTypeCode: String? = null,
    @Embedded
    val address: AddressDto,
)