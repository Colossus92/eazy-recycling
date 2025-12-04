package nl.eazysoftware.eazyrecyclingservice.domain.model.lmaimport

import java.util.*

/**
 * Result of an LMA CSV import operation.
 */
data class LmaImportResult(
  val importBatchId: UUID,
  val totalRows: Int,
  val successfulImports: Int,
  val skippedRows: Int,
  val errorCount: Int,
  val errors: List<LmaImportError>
) {
  val hasErrors: Boolean
    get() = errorCount > 0
}
