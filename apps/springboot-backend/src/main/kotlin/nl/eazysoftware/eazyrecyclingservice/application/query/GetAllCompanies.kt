package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.controller.company.CompanyController
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import org.springframework.stereotype.Service

interface GetAllCompanies {
  fun handle(includeBranches: Boolean = false): List<CompleteCompanyView>
}

@Service
class GetAllCompaniesQuery(
  private val companies: Companies,
  private val projectLocations: ProjectLocations,
) : GetAllCompanies {

  override fun handle(includeBranches: Boolean): List<CompleteCompanyView> {
    val companyViews = companies.findAll()
      .map { company -> CompleteCompanyView.fromDomain(company) }

    if (includeBranches) {
      val branches = projectLocations.findAll()
        .map { CompanyController.CompanyBranchResponse.from(it) }

      return companyViews.map { company ->
        val companyBranches = branches.filter { it.companyId == company.id }
        company.copy(branches = companyBranches)
      }
    }

    return companyViews
  }
}
