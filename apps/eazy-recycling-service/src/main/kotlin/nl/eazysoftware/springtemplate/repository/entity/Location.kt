package nl.eazysoftware.springtemplate.repository.entity

import jakarta.persistence.*

@Entity
data class Location(
    @Id
    val id: String,
    val description: String,
    val locationTypeCode: String,
    @Embedded
    val address: Address,
) {
}