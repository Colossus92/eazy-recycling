package nl.eazysoftware.eazyrecyclingservice.application.usecase.company

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyRole
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.VihbNumber
import java.util.*

data class CreateCompanyCommand(
  val name: String,
  val chamberOfCommerceId: String?,
  val vihbNumber: VihbNumber?,
  val processorId: ProcessorPartyId?,
  val address: Address,
  val roles: List<CompanyRole>,
)

data class UpdateCompanyCommand(
  val companyId: CompanyId,
  val name: String,
  val chamberOfCommerceId: String?,
  val vihbNumber: VihbNumber?,
  val processorId: ProcessorPartyId?,
  val address: Address,
  val roles: List<CompanyRole>,
)

data class CompanyResult(
  val companyId: UUID
)
