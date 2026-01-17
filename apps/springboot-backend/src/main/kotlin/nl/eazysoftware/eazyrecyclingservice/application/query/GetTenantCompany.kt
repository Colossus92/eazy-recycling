package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import org.springframework.stereotype.Service

interface GetTenantCompany {
  fun handle(): CompleteCompanyView?
}

@Service
class GetTenantCompanyQuery(
  private val companies: Companies,
) : GetTenantCompany {

  override fun handle(): CompleteCompanyView? {
    return companies.findTenantCompany()?.let { CompleteCompanyView.fromDomain(it) }
  }
}
