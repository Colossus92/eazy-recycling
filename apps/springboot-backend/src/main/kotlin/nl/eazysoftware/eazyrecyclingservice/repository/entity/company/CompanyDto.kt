package nl.eazysoftware.eazyrecyclingservice.repository.entity.company

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import java.time.Instant
import java.util.*

@Entity
@Table(name = "companies")
data class CompanyDto(
    @Id
    val id: UUID,

    @Column(unique = true, nullable = true)
    val chamberOfCommerceId: String? = null,

    @Column(unique = true, nullable = true)
    val vihbId: String? = null,

    @Column(name = "processor_id", unique = true, nullable = true)
    val processorId: String? = null,

    val name: String,

    @Embedded
    val address: AddressDto,

    @Column(name = "deleted_at", nullable = true)
    val deletedAt: Instant? = null,

    val updatedAt: Instant = Instant.now(),

    )
