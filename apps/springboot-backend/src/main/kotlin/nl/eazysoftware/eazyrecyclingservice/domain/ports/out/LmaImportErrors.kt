package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.lmaimport.LmaImportError
import java.util.*

/**
 * Domain port for LMA import error repository following hexagonal architecture.
 */
interface LmaImportErrors {
  fun save(error: LmaImportError): LmaImportError
  fun saveAll(errors: List<LmaImportError>): List<LmaImportError>
  fun findAll(): List<LmaImportError>
  fun findUnresolved(): List<LmaImportError>
  fun findByImportBatchId(importBatchId: UUID): List<LmaImportError>
  fun findById(id: UUID): LmaImportError?
  fun deleteAll()
  fun deleteByImportBatchId(importBatchId: UUID)
}
