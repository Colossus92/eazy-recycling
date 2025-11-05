package nl.eazysoftware.eazyrecyclingservice.application.usecase.company

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface CreateCompany {
  fun handle(cmd: CreateCompanyCommand): CompanyResult
}

@Service
class CreateCompanyService(
  private val companies: Companies,
) : CreateCompany {

  @Transactional
  override fun handle(cmd: CreateCompanyCommand): CompanyResult {
    // Validate uniqueness constraints
    cmd.chamberOfCommerceId?.let { kvk ->
      if (companies.existsByChamberOfCommerceId(kvk)) {
        throw DuplicateKeyException("KVK nummer $kvk is al in gebruik.")
      }
    }

    cmd.vihbNumber?.let { vihb ->
      if (companies.existsByVihbNumber(vihb.value)) {
        throw DuplicateKeyException("VIHB nummer ${vihb.value} is al in gebruik.")
      }
    }

    val company = Company(
      companyId = CompanyId(UUID.randomUUID()),
      name = cmd.name,
      chamberOfCommerceId = cmd.chamberOfCommerceId,
      vihbNumber = cmd.vihbNumber,
      processorId = cmd.processorId,
      address = cmd.address
    )

    val savedCompany = companies.create(company)

    return CompanyResult(companyId = savedCompany.companyId.uuid)
  }
}
