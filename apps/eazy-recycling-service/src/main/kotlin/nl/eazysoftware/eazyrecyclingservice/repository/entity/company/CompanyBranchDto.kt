package nl.eazysoftware.eazyrecyclingservice.repository.entity.company

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "company_branches")
data class CompanyBranchDto(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne
    @JoinColumn(name = "company_id", referencedColumnName = "id")
    val companyId: CompanyDto,

    @Embedded
    val address: AddressDto,

    val updatedAt: LocalDateTime = LocalDateTime.now(),

    )
