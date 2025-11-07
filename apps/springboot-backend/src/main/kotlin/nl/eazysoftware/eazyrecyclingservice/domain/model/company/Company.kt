package nl.eazysoftware.eazyrecyclingservice.domain.model.company

import kotlinx.datetime.Instant
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address

data class Company(
  val companyId: CompanyId,
  val name: String,
  val chamberOfCommerceId: String?,
  val vihbNumber: VihbNumber?,
  val processorId: ProcessorPartyId?,
  val address: Address,
  val deletedAt: Instant? = null,
)
