package nl.eazysoftware.eazyrecyclingservice.application.usecase.company

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface DeleteCompany {
  fun handle(companyId: CompanyId)
}

@Service
class DeleteCompanyService(
  private val companies: Companies,
) : DeleteCompany {

  @Transactional
  override fun handle(companyId: CompanyId) {
    // Verify company exists before deleting
    companies.findById(companyId)
      ?: throw EntityNotFoundException("Bedrijf met id ${companyId.uuid} niet gevonden")

    companies.deleteById(companyId)
  }
}
