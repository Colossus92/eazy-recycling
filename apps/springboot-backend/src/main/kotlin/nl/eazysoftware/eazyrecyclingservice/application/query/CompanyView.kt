package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.controller.company.CompanyController
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyRole
import org.springframework.data.domain.Page
import java.time.Instant
import java.util.*
import kotlin.time.toJavaInstant

/**
 * Paginated response for company list endpoint.
 */
data class PagedCompanyResponse(
  val content: List<CompleteCompanyView>,
  val page: Int,
  val size: Int,
  val totalElements: Long,
  val totalPages: Int,
) {
  companion object {
    fun from(page: Page<CompleteCompanyView>): PagedCompanyResponse {
      return PagedCompanyResponse(
        content = page.content,
        page = page.number,
        size = page.size,
        totalElements = page.totalElements,
        totalPages = page.totalPages,
      )
    }
  }
}

data class CompleteCompanyView(
  val id: UUID,
  val code: String?,
  val chamberOfCommerceId: String?,
  val vihbId: String?,
  val name: String,
  val address: AddressView,
  val processorId: String?,
  val branches: List<CompanyController.CompanyBranchResponse> = emptyList(),
  val roles: List<CompanyRole>,
  val phone: String?,
  val email: String?,
  val vatNumber: String?,
  val isTenantCompany: Boolean = false,
  val createdAt: Instant? = null,
  val createdByName: String? = null,
  val updatedAt: Instant? = null,
  val updatedByName: String? = null,
  ) {

  companion object {
    fun fromDomain(company: Company): CompleteCompanyView {
      return CompleteCompanyView(
        id = company.companyId.uuid,
        code = company.code,
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
        vatNumber = company.vatNumber,
        isTenantCompany = company.isTenantCompany,
        createdAt = company.createdAt?.toJavaInstant(),
        createdByName = company.createdBy,
        updatedAt = company.updatedAt?.toJavaInstant(),
        updatedByName = company.updatedBy,
      )
    }
  }
}
