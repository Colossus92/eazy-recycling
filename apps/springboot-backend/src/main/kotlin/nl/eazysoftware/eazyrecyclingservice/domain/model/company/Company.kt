package nl.eazysoftware.eazyrecyclingservice.domain.model.company

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import kotlin.time.Instant

data class Company(
  val companyId: CompanyId,
  val code: String? = null,
  val name: String,
  val chamberOfCommerceId: String?,
  val vihbNumber: VihbNumber?,
  val processorId: ProcessorPartyId?,
  val address: Address,
  val roles: List<CompanyRole> = emptyList(),
  val phone: PhoneNumber? = null,
  val email: Email? = null,
  val vatNumber: String? = null,
  val isSupplier: Boolean = true,
  val isCustomer: Boolean = true,
  val isTenantCompany: Boolean = false,
  val deletedAt: Instant? = null,
  val createdAt: Instant? = null,
  val createdBy: String? = null,
  val updatedAt: Instant? = null,
  val updatedBy: String? = null,
) {
  init {
    require(CompanyRole.PROCESSOR !in roles || processorId != null) {
      "Companies with PROCESSOR role must have a processorId"
    }
    require(CompanyRole.CARRIER !in roles || vihbNumber != null) {
      "Companies with CARRIER role must have a vihbNumber"
    }
  }
}
