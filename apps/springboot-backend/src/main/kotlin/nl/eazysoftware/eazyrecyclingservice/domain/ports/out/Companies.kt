package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId

interface Companies {
  fun create(company: Company): Company
  fun findAll(): List<Company>
  fun findById(companyId: CompanyId): Company?
  fun update(company: Company): Company
  fun deleteById(companyId: CompanyId)
  fun existsByChamberOfCommerceId(chamberOfCommerceId: String): Boolean
  fun existsByVihbNumber(vihbNumber: String): Boolean
  fun findByProcessorId(processorId: String): Company?
  fun findDeletedByChamberOfCommerceId(chamberOfCommerceId: String): Company?
  fun findDeletedByVihbNumber(vihbNumber: String): Company?
  fun findDeletedByProcessorId(processorId: String): Company?
  fun restore(companyId: CompanyId): Company
}
