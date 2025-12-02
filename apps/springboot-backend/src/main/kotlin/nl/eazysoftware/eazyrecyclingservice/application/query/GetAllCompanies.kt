package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.controller.company.CompanyController
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyRole
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.repository.exact.CompanySyncRepository
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

interface GetAllCompanies {

  /**
   * Search companies with pagination.
   * @param query Optional search query that matches against code, name, city, chamberOfCommerceId, and vihbId
   * @param role Optional role filter
   * @param page 0-indexed page number
   * @param size Page size
   * @param includeBranches Whether to include branches in the response
   * @return Paginated response with companies matching the criteria
   */
  fun searchPaginated(
    query: String?,
    role: CompanyRole?,
    page: Int,
    size: Int,
    includeBranches: Boolean = false
  ): PagedCompanyResponse
}

@Service
class GetAllCompaniesQuery(
  private val companies: Companies,
  private val projectLocations: ProjectLocations,
  private val companySyncRepository: CompanySyncRepository,
) : GetAllCompanies {

  override fun searchPaginated(
    query: String?,
    role: CompanyRole?,
    page: Int,
    size: Int,
    includeBranches: Boolean
  ): PagedCompanyResponse {
    val pageable = PageRequest.of(page, size)

    // Fetch all sync records to get external codes (needed for code search)
    val syncRecordsByCompanyId = companySyncRepository.findAll()
      .associateBy { it.companyId }

    // If query matches an external code, we need to handle it specially
    // since external codes are not stored in the company table
    val companyPage = companies.searchPaginated(query, role, pageable)

    // Map to views with external codes
    val companyViews = companyPage.content.map { company ->
      val externalCode = syncRecordsByCompanyId[company.companyId.uuid]?.externalId?.trim()
      CompleteCompanyView.fromDomain(company, externalCode)
    }

    // Filter by external code if query is provided (since DB doesn't have this field)
    val filteredViews = if (!query.isNullOrBlank()) {
      val lowerQuery = query.lowercase()
      companyViews.filter { view ->
        // Keep if already matched by DB query OR if external code matches
        view.externalCode?.lowercase()?.contains(lowerQuery) == true ||
        view.name.lowercase().contains(lowerQuery) ||
        view.chamberOfCommerceId?.lowercase()?.contains(lowerQuery) == true ||
        view.vihbId?.lowercase()?.contains(lowerQuery) == true ||
        view.address.city.lowercase().contains(lowerQuery)
      }
    } else {
      companyViews
    }

    // Add branches if requested
    val viewsWithBranches = if (includeBranches) {
      val branches = projectLocations.findAll()
        .map { CompanyController.CompanyBranchResponse.from(it) }

      filteredViews.map { company ->
        val companyBranches = branches.filter { it.companyId == company.id }
        company.copy(branches = companyBranches)
      }
    } else {
      filteredViews
    }

    // Create page with potentially filtered results
    val resultPage = PageImpl(viewsWithBranches, pageable, companyPage.totalElements)
    return PagedCompanyResponse.from(resultPage)
  }
}
