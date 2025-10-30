package nl.eazysoftware.eazyrecyclingservice.controller.exception

import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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


  @ExceptionHandler(IllegalStateException::class)
  fun handleIllegalStateException(ex: IllegalStateException): ResponseEntity<ErrorResponse> {
    val errorResponse = ErrorResponse(
      message = ex.message ?: "Deze actie brengt de data in een ongeldige staat en is niet toegestaan",
    )

    logException(ex)

    return ResponseEntity(errorResponse, HttpStatus.CONFLICT)
  }

  private fun logException(ex: Exception) {
    log.error("Exception caught by GlobalExceptionHandler: ${ex.javaClass.simpleName}", ex)
  }

  data class ErrorResponse(
    val message: String,
  )
}
