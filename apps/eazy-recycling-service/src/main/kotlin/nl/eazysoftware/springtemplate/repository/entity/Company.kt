package nl.eazysoftware.springtemplate.repository.entity

import jakarta.persistence.*
import java.util.*

@Entity
data class Company(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(unique = true, nullable = true)
    val chamberOfCommerceId: String? = null,

    @Column(unique = true, nullable = true)
    val vihbId: String? = null,
    val name: String,
    @Embedded
    val address: Address


)
