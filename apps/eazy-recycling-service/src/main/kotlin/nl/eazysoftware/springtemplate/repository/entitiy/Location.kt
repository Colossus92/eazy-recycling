package nl.eazysoftware.springtemplate.repository.entitiy

import jakarta.persistence.*

@Entity
data class Location(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val description: String,
    val locationTypeCode: String,
    @Embedded
    val address: Address,
) {
}