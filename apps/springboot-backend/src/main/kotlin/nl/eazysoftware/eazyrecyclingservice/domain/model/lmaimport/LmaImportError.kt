package nl.eazysoftware.eazyrecyclingservice.domain.model.lmaimport

import java.util.*
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Domain entity representing an error that occurred during LMA CSV import.
 * Errors are stored for user review and resolution.
 */
data class LmaImportError(
  val id: UUID = UUID.randomUUID(),
  val importBatchId: UUID,
  val rowNumber: Int,
  val wasteStreamNumber: String?,
  val errorCode: LmaImportErrorCode,
  val errorMessage: String,
  val rawData: Map<String, String>? = null,
  val createdAt: Instant = Clock.System.now(),
  val resolvedAt: Instant? = null,
  val resolvedBy: String? = null
) {
  fun resolve(resolvedBy: String): LmaImportError {
    return copy(
      resolvedAt = Clock.System.now(),
      resolvedBy = resolvedBy
    )
  }

  val isResolved: Boolean
    get() = resolvedAt != null
}

/**
 * Error codes for LMA import failures.
 */
enum class LmaImportErrorCode {
  /** KvK number doesn't match any company in the system */
  COMPANY_NOT_FOUND,
  /** Eural code format is invalid */
  INVALID_EURAL_CODE,
  /** Processing method code is invalid */
  INVALID_PROCESSING_METHOD,
  /** Waste stream number already exists */
  DUPLICATE_WASTE_STREAM,
  /** Required field is empty */
  MISSING_REQUIRED_FIELD,
  /** CSV structure doesn't match expected format */
  INVALID_CSV_FORMAT,
  /** Processor party not found */
  PROCESSOR_NOT_FOUND,
  /** General validation error */
  VALIDATION_ERROR
}
