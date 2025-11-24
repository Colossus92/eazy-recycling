package nl.eazysoftware.eazyrecyclingservice.domain.model.company

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import kotlin.time.Instant

data class Company(
  val companyId: CompanyId,
  val name: String,
  val chamberOfCommerceId: String?,
  val vihbNumber: VihbNumber?,
  val processorId: ProcessorPartyId?,
  val address: Address,
  val roles: List<CompanyRole> = emptyList(),
  val phone: String? = null,
  val email: String? = null,
  val isSupplier: Boolean = true,
  val isCustomer: Boolean = true,
  val deletedAt: Instant? = null,
)
