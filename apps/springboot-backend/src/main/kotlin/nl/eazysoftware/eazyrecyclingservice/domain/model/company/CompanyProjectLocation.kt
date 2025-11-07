package nl.eazysoftware.eazyrecyclingservice.domain.model.company

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import java.util.*
import kotlin.time.Clock
import kotlin.time.Instant

data class CompanyProjectLocation(
  val id: ProjectLocationId,
  val companyId: CompanyId,
  var address: Address,
  val createdAt: Instant = Clock.System.now(),
  var updatedAt: Instant? = null,
) {
  fun updateAddress(newAddress: Address) {
    this.address = newAddress
    this.updatedAt = Clock.System.now()
  }

  fun toSnapshot(): Location.ProjectLocationSnapshot {
    return Location.ProjectLocationSnapshot(
      projectLocationId = id,
      companyId = companyId,
      address = address,
    )
  }
}

data class ProjectLocationId(
  val uuid: UUID,
)
