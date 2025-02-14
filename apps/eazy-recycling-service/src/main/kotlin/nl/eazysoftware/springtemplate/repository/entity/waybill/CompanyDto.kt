package nl.eazysoftware.springtemplate.repository.entity.waybill

import jakarta.persistence.*
import java.util.*

@Entity
data class CompanyDto(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(unique = true, nullable = true)
    val chamberOfCommerceId: String? = null,

    @Column(unique = true, nullable = true)
    val vihbId: String? = null,
    val name: String,
    @Embedded
    val address: AddressDto


)
