package nl.eazysoftware.eazyrecyclingservice.domain.model.company

import nl.eazysoftware.eazyrecyclingservice.domain.transport.VihbNumber

data class Company(
  val companyId: CompanyId,
  val vihbNumber: VihbNumber,
)
