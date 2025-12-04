package nl.eazysoftware.eazyrecyclingservice.application.usecase.lmaimport

import nl.eazysoftware.eazyrecyclingservice.domain.model.lmaimport.LmaImportResult
import java.io.InputStream

/**
 * Use case interface for importing waste streams from LMA CSV export.
 */
interface ImportLmaWasteStreams {
  /**
   * Imports waste streams from an LMA CSV export file.
   *
   * @param csvInputStream The input stream of the CSV file
   * @return Import result containing success/error statistics
   */
  fun import(csvInputStream: InputStream): LmaImportResult
}
