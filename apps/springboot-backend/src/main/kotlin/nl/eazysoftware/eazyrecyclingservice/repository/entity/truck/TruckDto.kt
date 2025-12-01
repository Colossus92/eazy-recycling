package nl.eazysoftware.eazyrecyclingservice.repository.entity.truck

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.repository.AuditableEntity
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto

@Entity
@Table(name = "trucks")
data class TruckDto(
  @Id
  @Column(name = "license_plate", nullable = false)
  val licensePlate: String,

  @Column
  val brand: String? = null,

  @Column
  val description: String? = null,

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "carrier_party_id", referencedColumnName = "id")
  val carrierParty: CompanyDto? = null,
) : AuditableEntity() {

  fun getDisplayName(): String {
    val text = "$description ($licensePlate)"
    return carrierParty
      ?.let { "${it.name} - $text"  }
      ?: text
  }

  companion object {
    fun extractLicensePlateFromDisplayName(displayName: String): String {
      if (displayName.indexOf("(") == -1 || displayName.indexOf(")") == -1) {
        return displayName
      }
      return displayName.substring(displayName.indexOf("(") + 1, displayName.indexOf(")"))
    }
  }
}
