package nl.eazysoftware.eazyrecyclingservice.repository.company

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyRole
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

interface CompanyJpaRepository : JpaRepository<CompanyDto, UUID>, JpaSpecificationExecutor<CompanyDto> {

  fun findByVihbIdAndDeletedAtIsNotNull(id: String): CompanyDto?
  fun findByIdAndDeletedAtIsNull(id: UUID): CompanyDto?
  fun findByProcessorIdAndDeletedAtIsNull(processorId: String): CompanyDto?
  fun findByChamberOfCommerceIdAndDeletedAtIsNull(chamberOfCommerceId: String): CompanyDto?
  fun findByChamberOfCommerceIdAndDeletedAtNotNull(chamberOfCommerceId: String): CompanyDto?
  fun findByVihbIdAndDeletedAtNotNull(vihbId: String): CompanyDto?
  fun findByProcessorIdAndDeletedAtNotNull(processorId: String): CompanyDto?

  @Query("SELECT c FROM CompanyDto c JOIN c.roles r WHERE r = :role AND c.deletedAt IS NULL")
  fun findByRoleAndDeletedAtIsNull(@Param("role") role: CompanyRole): List<CompanyDto>

  @Query("""
    SELECT c FROM CompanyDto c
    WHERE c.address.postalCode = :postalCode
    AND c.address.buildingNumber = :buildingNumber
    AND ((:buildingNumberAddition IS NULL AND c.address.buildingNumberAddition IS NULL)
         OR c.address.buildingNumberAddition = :buildingNumberAddition)
    AND c.deletedAt IS NULL
  """)
  fun findByAddressAndDeletedAtIsNull(
    @Param("postalCode") postalCode: String,
    @Param("buildingNumber") buildingNumber: String,
    @Param("buildingNumberAddition") buildingNumberAddition: String?
  ): CompanyDto?
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

  override fun findByRole(role: CompanyRole): List<Company> {
    return jpaRepository.findByRoleAndDeletedAtIsNull(role)
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

  override fun findByChamberOfCommerceId(chamberOfCommerceId: String): Company? {
    return jpaRepository.findByChamberOfCommerceIdAndDeletedAtIsNull(chamberOfCommerceId)
      ?.let { companyMapper.toDomain(it) }
  }

  override fun existsByChamberOfCommerceId(chamberOfCommerceId: String): Boolean {
    return jpaRepository.findByChamberOfCommerceIdAndDeletedAtIsNull(chamberOfCommerceId) != null
  }

  override fun existsByVihbNumber(vihbNumber: String): Boolean {
    return jpaRepository.findByVihbIdAndDeletedAtIsNotNull(vihbNumber) != null
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

  override fun findByAddress(postalCode: String, buildingNumber: String, buildingNumberAddition: String?): Company? {
    return jpaRepository.findByAddressAndDeletedAtIsNull(postalCode, buildingNumber, buildingNumberAddition)
      ?.let { companyMapper.toDomain(it) }
  }

  override fun flush() {
    jpaRepository.flush()
  }

  override fun searchPaginated(query: String?, role: CompanyRole?, pageable: Pageable): Page<Company> {
    val spec = buildSearchSpecification(query, role)
    return jpaRepository.findAll(spec, pageable)
      .map { companyMapper.toDomain(it) }
  }

  private fun buildSearchSpecification(query: String?, role: CompanyRole?): Specification<CompanyDto> {
    return Specification { root, criteriaQuery, criteriaBuilder ->
      val predicates = mutableListOf(
        criteriaBuilder.isNull(root.get<Instant?>("deletedAt"))
      )

      // Role filter
      if (role != null) {
        predicates.add(criteriaBuilder.isMember(role, root.get("roles")))
      }

      // Search query filter - searches across code, name, city, chamberOfCommerceId, vihbId
      if (!query.isNullOrBlank()) {
        val searchPattern = "%${query.lowercase()}%"
        val searchPredicates = listOf(
          criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), searchPattern),
          criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchPattern),
          criteriaBuilder.like(criteriaBuilder.lower(root.get("chamberOfCommerceId")), searchPattern),
          criteriaBuilder.like(criteriaBuilder.lower(root.get("vihbId")), searchPattern),
          criteriaBuilder.like(criteriaBuilder.lower(root.get<Any>("address").get("city")), searchPattern),
        )
        predicates.add(criteriaBuilder.or(*searchPredicates.toTypedArray()))
      }

      // Order by code (numeric sort with nulls last), then by name ascending
      // LPAD pads with zeros for proper numeric sorting (code is trimmed at source)
      val codeField = root.get<String?>("code")
      
      // COALESCE handles nulls by replacing them with a high value string
      val paddedCode = criteriaBuilder.coalesce(
        criteriaBuilder.function("LPAD", String::class.java, codeField, criteriaBuilder.literal(10), criteriaBuilder.literal("0")),
        "9999999999" // High value for nulls to sort them last
      )
      
      criteriaQuery?.orderBy(
        criteriaBuilder.asc(paddedCode),
        criteriaBuilder.asc(root.get<String>("name"))
      )

      criteriaBuilder.and(*predicates.toTypedArray())
    }
  }
}
