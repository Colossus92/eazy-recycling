package nl.eazysoftware.eazyrecyclingservice.repository.company

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

interface CompanyJpaRepository : JpaRepository<CompanyDto, UUID> {

  fun findAllByDeletedAtIsNull(): List<CompanyDto>
  fun findByIdAndDeletedAtIsNull(id: UUID): CompanyDto?
  fun findByProcessorIdAndDeletedAtIsNull(processorId: String): CompanyDto?
  fun findByChamberOfCommerceIdAndDeletedAtNotNull(chamberOfCommerceId: String): CompanyDto?
  fun findByVihbIdAndDeletedAtNotNull(vihbId: String): CompanyDto?
  fun findByProcessorIdAndDeletedAtNotNull(processorId: String): CompanyDto?
}

@Repository
class CompanyRepository(
  private val jpaRepository: CompanyJpaRepository,
  private val companyMapper: CompanyMapper,
) : Companies {

  override fun create(company: Company): Company {
    val dto = companyMapper.toDto(company)
    val savedDto = jpaRepository.save(dto)
    return companyMapper.toDomain(savedDto)
  }

  override fun findAll(): List<Company> {
    return jpaRepository.findAllByDeletedAtIsNull()
      .map { companyMapper.toDomain(it) }
  }

  override fun findById(companyId: CompanyId): Company? {
    return jpaRepository.findByIdAndDeletedAtIsNull((companyId.uuid))
      ?.let { companyMapper.toDomain(it) }
  }

  override fun findByProcessorId(processorId: String) =
    jpaRepository.findByProcessorIdAndDeletedAtIsNull(processorId)
      ?.let { companyMapper.toDomain(it) }

  override fun update(company: Company): Company {
    val dto = companyMapper.toDto(company)
    val savedDto = jpaRepository.save(dto)
    return companyMapper.toDomain(savedDto)
  }

  override fun deleteById(companyId: CompanyId) {
    jpaRepository.findByIdAndDeletedAtIsNull((companyId.uuid))
      ?.let { jpaRepository.save(it.copy(deletedAt = Instant.now())) }
  }

  override fun existsByChamberOfCommerceId(chamberOfCommerceId: String): Boolean {
    return jpaRepository.findAllByDeletedAtIsNull()
      .any { it.chamberOfCommerceId == chamberOfCommerceId }
  }

  override fun existsByVihbNumber(vihbNumber: String): Boolean {
    return jpaRepository.findAllByDeletedAtIsNull()
      .any { it.vihbId == vihbNumber }
  }

  override fun findDeletedByChamberOfCommerceId(chamberOfCommerceId: String): Company? {
    return jpaRepository.findByChamberOfCommerceIdAndDeletedAtNotNull(chamberOfCommerceId)
      ?.let { companyMapper.toDomain(it) }
  }

  override fun findDeletedByVihbNumber(vihbNumber: String): Company? {
    return jpaRepository.findByVihbIdAndDeletedAtNotNull(vihbNumber)
      ?.let { companyMapper.toDomain(it) }
  }

  override fun findDeletedByProcessorId(processorId: String): Company? {
    return jpaRepository.findByProcessorIdAndDeletedAtNotNull(processorId)
      ?.let { companyMapper.toDomain(it) }
  }

  override fun restore(companyId: CompanyId): Company {
    val dto = jpaRepository.findByIdOrNull(companyId.uuid)
      ?: throw IllegalArgumentException("Bedrijf met id ${companyId.uuid} niet gevonden")
    if (dto.deletedAt == null) {
      throw IllegalStateException("Bedrijf met id ${companyId.uuid} is niet verwijderd")
    }
    val restoredDto = jpaRepository.save(dto.copy(deletedAt = null))
    return companyMapper.toDomain(restoredDto)
  }
}
