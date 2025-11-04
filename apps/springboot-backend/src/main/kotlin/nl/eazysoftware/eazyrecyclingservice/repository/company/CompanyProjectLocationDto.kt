package nl.eazysoftware.eazyrecyclingservice.repository.company

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import java.time.Instant
import java.util.*

@Entity
@Table(name = "company_project_locations")
class CompanyProjectLocationDto (
  @Id
  val id: UUID,

  @Column(name = "street_name", nullable = false)
  val streetName: String,

  @Column(name = "building_number", nullable = false)
  val buildingNumber: String,

  @Column(name = "building_number_addition")
  val buildingNumberAddition: String?,

  @Column(name = "city", nullable = false)
  val city: String,

  @Column(name = "postal_code", nullable = false)
  val postalCode: String,

  @Column(name = "country", nullable = false)
  val country: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", nullable = false)
  val company: CompanyDto,

  @Column(name = "created_at", nullable = false)
  val createdAt: Instant,

  @Column(name = "updated_at")
  val updatedAt: Instant?,
)
