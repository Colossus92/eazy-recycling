package nl.eazysoftware.springtemplate.repository.entitiy

import jakarta.persistence.*

@Entity
data class Location(
    @Id
    val id: String? = null,
    val description: String,
    val locationTypeCode: String,
    @Embedded
    val address: Address,
) {
}