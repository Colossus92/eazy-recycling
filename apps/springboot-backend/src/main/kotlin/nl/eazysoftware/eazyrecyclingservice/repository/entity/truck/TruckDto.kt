package nl.eazysoftware.eazyrecyclingservice.repository.entity.truck

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import java.time.LocalDateTime

@Entity
@Table(name = "trucks")
data class TruckDto(
  @Id
  @Column(name = "license_plate", nullable = false)
  val licensePlate: String,

  @Column
  val brand: String? = null,

  @Column
  val model: String? = null,

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "carrier_party_id", referencedColumnName = "id")
  val carrierParty: CompanyDto? = null,

  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now()
) {
  @PrePersist
  fun prePersist() {
    updatedAt = LocalDateTime.now()
  }

  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }

  fun getDisplayName(): String {
    val text = "$model ($licensePlate)"
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
