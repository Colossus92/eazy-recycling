package nl.eazysoftware.eazyrecyclingservice.repository.entity.company

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "companies")
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
    val address: AddressDto,

    val updatedAt: LocalDateTime = LocalDateTime.now(),

    )