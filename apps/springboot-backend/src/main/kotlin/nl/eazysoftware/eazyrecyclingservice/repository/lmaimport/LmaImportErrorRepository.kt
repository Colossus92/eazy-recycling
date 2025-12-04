package nl.eazysoftware.eazyrecyclingservice.repository.lmaimport

import nl.eazysoftware.eazyrecyclingservice.domain.model.lmaimport.LmaImportError
import nl.eazysoftware.eazyrecyclingservice.domain.model.lmaimport.LmaImportErrorCode
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaImportErrors
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

interface LmaImportErrorJpaRepository : JpaRepository<LmaImportErrorDto, UUID> {
  fun findByResolvedAtIsNull(): List<LmaImportErrorDto>
  fun findByImportBatchId(importBatchId: UUID): List<LmaImportErrorDto>
  fun deleteByImportBatchId(importBatchId: UUID)
}

@Repository
class LmaImportErrorRepository(
  private val jpaRepository: LmaImportErrorJpaRepository
) : LmaImportErrors {

  override fun save(error: LmaImportError): LmaImportError {
    val dto = toDto(error)
    val saved = jpaRepository.save(dto)
    return toDomain(saved)
  }

  override fun saveAll(errors: List<LmaImportError>): List<LmaImportError> {
    val dtos = errors.map { toDto(it) }
    val saved = jpaRepository.saveAll(dtos)
    return saved.map { toDomain(it) }
  }

  override fun findAll(): List<LmaImportError> {
    return jpaRepository.findAll().map { toDomain(it) }
  }

  override fun findUnresolved(): List<LmaImportError> {
    return jpaRepository.findByResolvedAtIsNull().map { toDomain(it) }
  }

  override fun findByImportBatchId(importBatchId: UUID): List<LmaImportError> {
    return jpaRepository.findByImportBatchId(importBatchId).map { toDomain(it) }
  }

  override fun findById(id: UUID): LmaImportError? {
    return jpaRepository.findById(id).orElse(null)?.let { toDomain(it) }
  }

  override fun deleteAll() {
    jpaRepository.deleteAll()
  }

  override fun deleteByImportBatchId(importBatchId: UUID) {
    jpaRepository.deleteByImportBatchId(importBatchId)
  }

  private fun toDto(domain: LmaImportError): LmaImportErrorDto {
    return LmaImportErrorDto(
      id = domain.id,
      importBatchId = domain.importBatchId,
      rowNumber = domain.rowNumber,
      wasteStreamNumber = domain.wasteStreamNumber,
      errorCode = domain.errorCode.name,
      errorMessage = domain.errorMessage,
      rawData = domain.rawData,
      createdAt = domain.createdAt.toJavaInstant(),
      resolvedAt = domain.resolvedAt?.toJavaInstant(),
      resolvedBy = domain.resolvedBy
    )
  }

  private fun toDomain(dto: LmaImportErrorDto): LmaImportError {
    return LmaImportError(
      id = dto.id,
      importBatchId = dto.importBatchId,
      rowNumber = dto.rowNumber,
      wasteStreamNumber = dto.wasteStreamNumber,
      errorCode = LmaImportErrorCode.valueOf(dto.errorCode),
      errorMessage = dto.errorMessage,
      rawData = dto.rawData,
      createdAt = dto.createdAt.toKotlinInstant(),
      resolvedAt = dto.resolvedAt?.toKotlinInstant(),
      resolvedBy = dto.resolvedBy
    )
  }
}
