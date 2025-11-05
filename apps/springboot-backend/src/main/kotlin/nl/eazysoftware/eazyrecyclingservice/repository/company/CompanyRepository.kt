package nl.eazysoftware.eazyrecyclingservice.repository.company

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.util.*

interface CompanyJpaRepository: JpaRepository<CompanyDto, UUID> {

    fun findByProcessorId(processorId: String): CompanyDto?
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
    return jpaRepository.findAll()
      .map { companyMapper.toDomain(it) }
  }

  override fun findById(companyId: CompanyId): Company? {
    return jpaRepository.findByIdOrNull(companyId.uuid)
      ?.let { companyMapper.toDomain(it) }
  }

  override fun findByProcessorId(processorId: String) =
    jpaRepository.findByProcessorId(processorId)
      ?.let { companyMapper.toDomain(it) }

  override fun update(company: Company): Company {
    val dto = companyMapper.toDto(company)
    val savedDto = jpaRepository.save(dto)
    return companyMapper.toDomain(savedDto)
  }

  override fun deleteById(companyId: CompanyId) {
    jpaRepository.deleteById(companyId.uuid)
  }

  override fun existsByChamberOfCommerceId(chamberOfCommerceId: String): Boolean {
    return jpaRepository.findAll()
      .any { it.chamberOfCommerceId == chamberOfCommerceId }
  }

  override fun existsByVihbNumber(vihbNumber: String): Boolean {
    return jpaRepository.findAll()
      .any { it.vihbId == vihbNumber }
  }
}
