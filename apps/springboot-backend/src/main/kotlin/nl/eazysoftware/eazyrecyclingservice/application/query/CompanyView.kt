package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.controller.company.CompanyController
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyRole
import java.time.Instant
import java.util.*
import kotlin.time.toJavaInstant

data class CompleteCompanyView(
  val id: UUID,
  val externalCode: String?,
  val chamberOfCommerceId: String?,
  val vihbId: String?,
  val name: String,
  val address: AddressView,
  val processorId: String?,
  val branches: List<CompanyController.CompanyBranchResponse> = emptyList(),
  val roles: List<CompanyRole>,
  val phone: String?,
  val email: String?,
  val createdAt: Instant? = null,
  val createdByName: String? = null,
  val updatedAt: Instant? = null,
  val updatedByName: String? = null,
  ) {

  companion object {
    fun fromDomain(company: Company, externalCode: String? = null): CompleteCompanyView {
      return CompleteCompanyView(
        id = company.companyId.uuid,
        externalCode = externalCode,
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
        roles = company.roles,
        phone = company.phone?.value,
        email = company.email?.value,
        createdAt = company.createdAt?.toJavaInstant(),
        createdByName = company.createdBy,
        updatedAt = company.updatedAt?.toJavaInstant(),
        updatedByName = company.updatedBy,
      )
    }
  }
}
