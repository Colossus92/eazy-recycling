package nl.eazysoftware.eazyrecyclingservice.controller.exception

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.application.usecase.company.SoftDeletedCompanyConflictException
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.IncompatibleWasteStreamsException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {

  private val log: Logger = LoggerFactory.getLogger(this::class.java)

  @ExceptionHandler(DuplicateKeyException::class)
  fun handleDuplicateKeyException(ex: DuplicateKeyException): ResponseEntity<ErrorResponse> {
    val errorResponse = ErrorResponse(
      message = ex.message ?: "Object met id bestaat al",
    )

    logException(ex)

    return ResponseEntity(errorResponse, HttpStatus.CONFLICT)
  }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFoundException(ex: EntityNotFoundException): ResponseEntity<ErrorResponse> {
    val errorResponse = ErrorResponse(
      message = ex.message ?: "Object niet gevonden",
    )
    logException(ex)

    return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
  }

  @ExceptionHandler(IllegalArgumentException::class)
  fun handleDuplicateKeyException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
    val errorResponse = ErrorResponse(
      message = ex.message ?: "Ongeldige invoer",
    )

    logException(ex)

    return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
  }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
    val fieldErrors = ex.bindingResult.fieldErrors.joinToString(", ") { "${it.defaultMessage}" }
    val globalErrors = ex.bindingResult.globalErrors.joinToString(", ") { "${it.defaultMessage}" }
    
    val allErrors = listOf(fieldErrors, globalErrors)
      .filter { it.isNotEmpty() }
      .joinToString(", ")

    val errorResponse = ErrorResponse(
      message = allErrors.ifEmpty { "Validatie mislukt" },
    )

    logException(ex)

    return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
  }


  @ExceptionHandler(IllegalStateException::class)
  fun handleIllegalStateException(ex: IllegalStateException): ResponseEntity<ErrorResponse> {
    val errorResponse = ErrorResponse(
      message = ex.message ?: "Deze actie brengt de data in een ongeldige staat en is niet toegestaan",
    )

    logException(ex)

    return ResponseEntity(errorResponse, HttpStatus.CONFLICT)
  }

  @ExceptionHandler(SoftDeletedCompanyConflictException::class)
  fun handleSoftDeletedCompanyConflictException(ex: SoftDeletedCompanyConflictException): ResponseEntity<SoftDeletedCompanyConflictResponse> {
    val response = SoftDeletedCompanyConflictResponse(
      message = ex.message,
      deletedCompanyId = ex.deletedCompanyId.uuid.toString(),
      conflictField = ex.conflictField,
      conflictValue = ex.conflictValue
    )

    logException(ex)

    return ResponseEntity(response, HttpStatus.CONFLICT)
  }

  @ExceptionHandler(IncompatibleWasteStreamsException::class)
  fun handleIncompatibleWasteStreamsException(ex: IncompatibleWasteStreamsException): ResponseEntity<ErrorResponse> {
    val errorResponse = ErrorResponse(
      message = ex.message ?: "Deze actie brengt de data in een ongeldige staat en is niet toegestaan",
    )

    logException(ex)

    return ResponseEntity(errorResponse, HttpStatus.CONFLICT)
  }

  @ExceptionHandler(DataIntegrityViolationException::class)
  fun handleDataIntegrityViolationException(ex: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
    // Check if this is a unique constraint violation by examining the message
    val message = ex.message ?: ""
    val rootCause = ex.rootCause?.message ?: ""
    
    val errorMessage = when {
      message.contains("duplicate key", ignoreCase = true) || 
      rootCause.contains("duplicate key", ignoreCase = true) -> {
        // Extract constraint name if available for better error messages
        val constraintMatch = Regex("constraint \"([^\"]+)\"").find(rootCause)
        val constraintName = constraintMatch?.groupValues?.get(1)
        
        when {
          constraintName?.contains("code") == true -> "Een item met deze code bestaat al"
          constraintName?.contains("name") == true -> "Een item met deze naam bestaat al"
          else -> "Dit item bestaat al in de database"
        }
      }
      message.contains("foreign key", ignoreCase = true) || 
      rootCause.contains("foreign key", ignoreCase = true) -> {
        "Deze actie kan niet worden uitgevoerd omdat er gerelateerde gegevens bestaan"
      }
      else -> "Er is een probleem met de gegevensintegriteit"
    }

    val errorResponse = ErrorResponse(message = errorMessage)
    logException(ex)

    return ResponseEntity(errorResponse, HttpStatus.CONFLICT)
  }

  private fun logException(ex: Exception) {
    log.error("Exception caught by GlobalExceptionHandler: ${ex.javaClass.simpleName}", ex)
  }

  data class ErrorResponse(
    val message: String,
  )

  data class SoftDeletedCompanyConflictResponse(
    val message: String,
    val deletedCompanyId: String,
    val conflictField: String,
    val conflictValue: String
  )
}
