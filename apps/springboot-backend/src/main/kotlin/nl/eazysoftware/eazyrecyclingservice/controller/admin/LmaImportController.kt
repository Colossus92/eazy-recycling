package nl.eazysoftware.eazyrecyclingservice.controller.admin

import nl.eazysoftware.eazyrecyclingservice.application.usecase.lmaimport.ImportLmaWasteStreams
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ROLE_ADMIN
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaImportErrors
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

/**
 * Controller for LMA CSV import functionality.
 * Allows importing waste streams from LMA portal exports.
 */
@RestController
@RequestMapping("/admin/lma")
class LmaImportController(
  private val importLmaWasteStreams: ImportLmaWasteStreams,
  private val lmaImportErrors: LmaImportErrors
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  /**
   * POST /api/admin/lma/import
   *
   * Upload and process an LMA CSV export file.
   * The file should be a CSV export from the LMA portal (converted from xlsx).
   */
  @PreAuthorize(HAS_ROLE_ADMIN)
  @PostMapping("/import", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  fun importCsv(@RequestParam("file") file: MultipartFile): ResponseEntity<LmaImportResponse> {
    logger.info("Received LMA import request: ${file.originalFilename}, size: ${file.size} bytes")

    if (file.isEmpty) {
      return ResponseEntity.badRequest().body(
        LmaImportResponse(
          success = false,
          message = "Bestand is leeg",
          importBatchId = null,
          totalRows = 0,
          successfulImports = 0,
          skippedRows = 0,
          errorCount = 0
        )
      )
    }

    val result = importLmaWasteStreams.import(file.inputStream)

    val message = if (result.hasErrors) {
      "Import voltooid met ${result.errorCount} fouten"
    } else {
      "Import succesvol voltooid"
    }

    return ResponseEntity.ok(
      LmaImportResponse(
        success = !result.hasErrors,
        message = message,
        importBatchId = result.importBatchId,
        totalRows = result.totalRows,
        successfulImports = result.successfulImports,
        skippedRows = result.skippedRows,
        errorCount = result.errorCount
      )
    )
  }

  /**
   * GET /api/admin/lma/import/errors
   *
   * Get all unresolved import errors.
   */
  @PreAuthorize(HAS_ROLE_ADMIN)
  @GetMapping("/import/errors")
  fun getErrors(): ResponseEntity<LmaImportErrorsResponse> {
    val errors = lmaImportErrors.findUnresolved()
    return ResponseEntity.ok(
      LmaImportErrorsResponse(
        errors = errors.map { LmaImportErrorDto.fromDomain(it) }
      )
    )
  }

  /**
   * GET /api/admin/lma/import/errors/{batchId}
   *
   * Get all errors for a specific import batch.
   */
  @PreAuthorize(HAS_ROLE_ADMIN)
  @GetMapping("/import/errors/{batchId}")
  fun getErrorsByBatch(@PathVariable batchId: UUID): ResponseEntity<LmaImportErrorsResponse> {
    val errors = lmaImportErrors.findByImportBatchId(batchId)
    return ResponseEntity.ok(
      LmaImportErrorsResponse(
        errors = errors.map { LmaImportErrorDto.fromDomain(it) }
      )
    )
  }

  /**
   * DELETE /api/admin/lma/import/errors/{batchId}
   *
   * Delete all errors for a specific import batch.
   */
  @PreAuthorize(HAS_ROLE_ADMIN)
  @DeleteMapping("/import/errors/{batchId}")
  fun deleteErrorsByBatch(@PathVariable batchId: UUID): ResponseEntity<Void> {
    lmaImportErrors.deleteByImportBatchId(batchId)
    return ResponseEntity.noContent().build()
  }

  // Response DTOs

  data class LmaImportResponse(
    val success: Boolean,
    val message: String,
    val importBatchId: UUID?,
    val totalRows: Int,
    val successfulImports: Int,
    val skippedRows: Int,
    val errorCount: Int
  )

  data class LmaImportErrorsResponse(
    val errors: List<LmaImportErrorDto>
  )

  data class LmaImportErrorDto(
    val id: String,
    val importBatchId: String,
    val rowNumber: Int,
    val wasteStreamNumber: String?,
    val errorCode: String,
    val errorMessage: String,
    val rawData: Map<String, String>?,
    val createdAt: String,
    val resolvedAt: String?,
    val resolvedBy: String?
  ) {
    companion object {
      fun fromDomain(domain: nl.eazysoftware.eazyrecyclingservice.domain.model.lmaimport.LmaImportError): LmaImportErrorDto {
        return LmaImportErrorDto(
          id = domain.id.toString(),
          importBatchId = domain.importBatchId.toString(),
          rowNumber = domain.rowNumber,
          wasteStreamNumber = domain.wasteStreamNumber,
          errorCode = domain.errorCode.name,
          errorMessage = domain.errorMessage,
          rawData = domain.rawData,
          createdAt = domain.createdAt.toString(),
          resolvedAt = domain.resolvedAt?.toString(),
          resolvedBy = domain.resolvedBy
        )
      }
    }
  }
}
