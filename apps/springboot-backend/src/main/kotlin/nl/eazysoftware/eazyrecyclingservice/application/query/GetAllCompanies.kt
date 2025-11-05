package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.domain.service.CompanyService
import org.springframework.stereotype.Service

interface GetAllCompanies {
  fun handle(includeBranches: Boolean = false): List<CompanyService.CompanyView>
}

@Service
class GetAllCompaniesQuery(
  private val companies: Companies,
  private val projectLocations: ProjectLocations,
) : GetAllCompanies {

  override fun handle(includeBranches: Boolean): List<CompanyService.CompanyView> {
    val companyViews = companies.findAll()
      .map { company -> CompanyService.CompanyView.fromDomain(company) }

    if (includeBranches) {
      val branches = projectLocations.findAll()
        .map { CompanyService.CompanyBranchResponse.from(it) }

      return companyViews.map { company ->
        val companyBranches = branches.filter { it.companyId == company.id }
        company.copy(branches = companyBranches)
      }
    }

    return companyViews
  }
}
