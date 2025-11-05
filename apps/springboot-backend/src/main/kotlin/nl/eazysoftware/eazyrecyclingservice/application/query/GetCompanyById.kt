package nl.eazysoftware.eazyrecyclingservice.application.query

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.service.CompanyService
import org.springframework.stereotype.Service

interface GetCompanyById {
  fun handle(companyId: CompanyId): CompanyService.CompanyView
}

@Service
class GetCompanyByIdQuery(
  private val companies: Companies,
) : GetCompanyById {

  override fun handle(companyId: CompanyId): CompanyService.CompanyView {
    val company = companies.findById(companyId)
      ?: throw EntityNotFoundException("Bedrijf met id ${companyId.uuid} niet gevonden")

    return CompanyService.CompanyView.fromDomain(company)
  }
}
