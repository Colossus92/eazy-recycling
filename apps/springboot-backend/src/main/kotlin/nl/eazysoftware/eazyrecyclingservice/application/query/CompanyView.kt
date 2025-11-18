package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.controller.company.CompanyController
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyRole
import java.time.LocalDateTime
import java.util.*

data class CompleteCompanyView(
  val id: UUID,
  val chamberOfCommerceId: String?,
  val vihbId: String?,
  val name: String,
  val address: AddressView,
  val processorId: String?,
  val updatedAt: LocalDateTime,
  val branches: List<CompanyController.CompanyBranchResponse> = emptyList(),
  val roles: List<CompanyRole>,
  ) {

  companion object {
    fun fromDomain(company: Company): CompleteCompanyView {
      return CompleteCompanyView(
        id = company.companyId.uuid,
        chamberOfCommerceId = company.chamberOfCommerceId,
        vihbId = company.vihbNumber?.value,
        name = company.name,
        address = AddressView(
          street = company.address.streetName.value,
          houseNumber = company.address.buildingNumber,
          houseNumberAddition = company.address.buildingNumberAddition,
          postalCode = company.address.postalCode.value,
          city = company.address.city.value,
          country = company.address.country
        ),
        processorId = company.processorId?.number,
        updatedAt = LocalDateTime.now(),
        roles = company.roles,
      )
    }
  }
}
