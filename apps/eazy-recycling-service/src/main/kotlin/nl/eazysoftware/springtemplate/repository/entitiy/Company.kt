package nl.eazysoftware.springtemplate.repository.entitiy

import jakarta.persistence.*

@Entity
data class Company(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = true)
    val chamberOfCommerceId: String? = null,

    @Column(unique = true, nullable = true)
    val vihbId: String? = null,
    val name: String,
    @Embedded
    val address: Address


)
