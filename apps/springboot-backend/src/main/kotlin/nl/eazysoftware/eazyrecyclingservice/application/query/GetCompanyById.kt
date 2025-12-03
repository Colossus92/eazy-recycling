package nl.eazysoftware.eazyrecyclingservice.application.query

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import org.springframework.stereotype.Service

interface GetCompanyById {
  fun handle(companyId: CompanyId): CompleteCompanyView
}

@Service
class GetCompanyByIdQuery(
  private val companies: Companies,
) : GetCompanyById {

  override fun handle(companyId: CompanyId): CompleteCompanyView {
    val company = companies.findById(companyId)
      ?: throw EntityNotFoundException("Bedrijf met id ${companyId.uuid} niet gevonden")

    return CompleteCompanyView.fromDomain(company)
  }
}
