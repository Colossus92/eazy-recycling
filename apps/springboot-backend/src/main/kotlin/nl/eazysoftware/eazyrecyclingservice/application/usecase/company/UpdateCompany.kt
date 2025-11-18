package nl.eazysoftware.eazyrecyclingservice.application.usecase.company

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface UpdateCompany {
  fun handle(cmd: UpdateCompanyCommand): CompanyResult
  fun handleRestore(companyId: CompanyId, cmd: UpdateCompanyCommand): CompanyResult
}

@Service
class UpdateCompanyService(
  private val companies: Companies,
) : UpdateCompany {

  @Transactional
  override fun handle(cmd: UpdateCompanyCommand): CompanyResult {
    // Verify company exists
    val existingCompany = companies.findById(cmd.companyId)
      ?: throw EntityNotFoundException("Bedrijf met id ${cmd.companyId.uuid} niet gevonden")

    // Validate uniqueness constraints for fields that changed
    cmd.chamberOfCommerceId?.let { kvk ->
      if (kvk != existingCompany.chamberOfCommerceId && companies.existsByChamberOfCommerceId(kvk)) {
        throw DuplicateKeyException("KVK nummer $kvk is al in gebruik.")
      }
    }

    cmd.vihbNumber?.let { vihb ->
      if (vihb != existingCompany.vihbNumber && companies.existsByVihbNumber(vihb.value)) {
        throw DuplicateKeyException("VIHB nummer ${vihb.value} is al in gebruik.")
      }
    }

    val updatedCompany = Company(
      companyId = cmd.companyId,
      name = cmd.name,
      chamberOfCommerceId = cmd.chamberOfCommerceId,
      vihbNumber = cmd.vihbNumber,
      processorId = cmd.processorId,
      address = cmd.address,
      roles = cmd.roles,
    )

    companies.update(updatedCompany)

    return CompanyResult(companyId = cmd.companyId.uuid)
  }

  @Transactional
  override fun handleRestore(companyId: CompanyId, cmd: UpdateCompanyCommand): CompanyResult {
    // First, restore the soft-deleted company
    companies.restore(companyId)

    // Then update it with the new data
    return handle(cmd)
  }
}
