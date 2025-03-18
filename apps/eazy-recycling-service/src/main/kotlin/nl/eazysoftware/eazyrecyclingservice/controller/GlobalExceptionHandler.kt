package nl.eazysoftware.eazyrecyclingservice.controller

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<Map<String, String>> {
        log.error("An error occurred", ex)
        val errorResponse = mapOf("message" to (ex.message ?: "An error occurred"))

        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(ex: EntityNotFoundException): ResponseEntity<Map<String, String>> {
        log.error("Entity could not be found:", ex)
        val errorResponse = mapOf("message" to (ex.message ?: "An error occurred"))

        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    }
}