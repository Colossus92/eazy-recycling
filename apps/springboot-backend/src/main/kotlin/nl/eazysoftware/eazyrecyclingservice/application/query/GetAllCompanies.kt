package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.config.cache.CacheConfig
import nl.eazysoftware.eazyrecyclingservice.controller.company.CompanyController
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyRole
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import org.springframework.cache.annotation.Cacheable
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
   * @param sortBy Optional field to sort by (code, name)
   * @param sortDirection Sort direction (asc, desc)
   * @param excludeTenant Whether to exclude the tenant company from results
   * @return Paginated response with companies matching the criteria
   */
  fun searchPaginated(
    query: String?,
    role: CompanyRole?,
    page: Int,
    size: Int,
    includeBranches: Boolean = false,
    sortBy: String? = null,
    sortDirection: String = "asc",
    excludeTenant: Boolean = false,
  ): PagedCompanyResponse
}

@Service
class GetAllCompaniesQuery(
  private val companies: Companies,
  private val projectLocations: ProjectLocations,
) : GetAllCompanies {

  @Cacheable(
    cacheNames = [CacheConfig.COMPANIES_CACHE],
    key = "'search:' + #query + ':' + #role + ':' + #page + ':' + #size + ':' + #includeBranches + ':' + #sortBy + ':' + #sortDirection + ':' + #excludeTenant"
  )
  override fun searchPaginated(
    query: String?,
    role: CompanyRole?,
    page: Int,
    size: Int,
    includeBranches: Boolean,
    sortBy: String?,
    sortDirection: String,
    excludeTenant: Boolean
  ): PagedCompanyResponse {
    val pageable = PageRequest.of(page, size)

    // Fetch companies with code already included
    val companyPage = companies.searchPaginated(query, role, pageable, sortBy, sortDirection, excludeTenant)

    // Map to views
    val companyViews = companyPage.content.map { company ->
      CompleteCompanyView.fromDomain(company)
    }

    // Add branches if requested
    val viewsWithBranches = if (includeBranches) {
      val branchesByCompanyId = projectLocations.findAll()
        .map { CompanyController.CompanyBranchResponse.from(it) }
        .groupBy { it.companyId }

      companyViews.map { company ->
        company.copy(branches = branchesByCompanyId[company.id] ?: emptyList())
      }
    } else {
      companyViews
    }

    // Create page with potentially filtered results
    val resultPage = PageImpl(viewsWithBranches, pageable, companyPage.totalElements)
    return PagedCompanyResponse.from(resultPage)
  }
}
