package nl.eazysoftware.eazyrecyclingservice.repository.entity.company

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyRole
import nl.eazysoftware.eazyrecyclingservice.repository.AuditableEntity
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

  @ElementCollection(targetClass = CompanyRole::class, fetch = FetchType.EAGER)
  @CollectionTable(name = "company_roles", joinColumns = [JoinColumn(name = "company_id")])
  @Column(name = "roles", nullable = false)
  @Enumerated(EnumType.STRING)
  val roles: List<CompanyRole> = emptyList(),

  @Column(name = "phone", nullable = true)
  val phone: String? = null,

  @Column(name = "email", nullable = true)
  val email: String? = null,

  @Column(name = "is_supplier", nullable = false)
  val isSupplier: Boolean = true,

  @Column(name = "is_customer", nullable = false)
  val isCustomer: Boolean = true,

  @Column(name = "deleted_at", nullable = true)
  val deletedAt: Instant? = null,

  /**
   * User ID who deleted the company (metadata only, not used in queries).
   */
  @Column(name = "deleted_by", nullable = true)
  val deletedBy: UUID? = null,
) : AuditableEntity()
