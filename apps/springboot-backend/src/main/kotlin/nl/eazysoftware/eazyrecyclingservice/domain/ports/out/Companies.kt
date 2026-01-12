package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface Companies {
  fun create(company: Company): Company
  /**
   * Search companies with pagination.
   * @param query Optional search query that matches against code, name, city, chamberOfCommerceId, and vihbId
   * @param role Optional role filter
   * @param pageable Pagination parameters
   * @return Page of companies matching the criteria
   */
  fun searchPaginated(query: String?, role: CompanyRole?, pageable: Pageable): Page<Company>
  fun findByRole(role: CompanyRole): List<Company>
  fun findById(companyId: CompanyId): Company?
  fun update(company: Company): Company
  fun deleteById(companyId: CompanyId)
  fun findByChamberOfCommerceId(chamberOfCommerceId: String): Company?
  fun findAllByChamberOfCommerceId(chamberOfCommerceId: String): List<Company>
  fun existsByChamberOfCommerceId(chamberOfCommerceId: String): Boolean
  fun existsByVihbNumber(vihbNumber: String): Boolean
  fun findByProcessorId(processorId: String): Company?
  fun findDeletedByChamberOfCommerceId(chamberOfCommerceId: String): Company?
  fun findDeletedByVihbNumber(vihbNumber: String): Company?
  fun findDeletedByProcessorId(processorId: String): Company?
  fun restore(companyId: CompanyId): Company

  /**
   * Find company by exact address match (postal code, building number, building number addition).
   * Used for matching companies from Exact Online when no other identifiers are available.
   */
  fun findByAddress(postalCode: String, buildingNumber: String, buildingNumberAddition: String?): Company?

  /**
   * Flush pending changes to the database.
   * Used to detect constraint violations immediately rather than at commit time.
   */
  fun flush()
}
