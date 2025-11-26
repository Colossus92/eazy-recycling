package nl.eazysoftware.eazyrecyclingservice.application.query

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.repository.exact.CompanySyncRepository
import org.springframework.stereotype.Service

interface GetCompanyById {
  fun handle(companyId: CompanyId): CompleteCompanyView
}

@Service
class GetCompanyByIdQuery(
  private val companies: Companies,
  private val companySyncRepository: CompanySyncRepository,
) : GetCompanyById {

  override fun handle(companyId: CompanyId): CompleteCompanyView {
    val company = companies.findById(companyId)
      ?: throw EntityNotFoundException("Bedrijf met id ${companyId.uuid} niet gevonden")

    val externalCode = companySyncRepository.findByCompanyId(companyId.uuid)?.externalId
    return CompleteCompanyView.fromDomain(company, externalCode)
  }
}
