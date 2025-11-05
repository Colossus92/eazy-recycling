package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream

import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreamValidationResult
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreamValidator
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Use case for validating a waste stream against the external Amice validation service.
 * This validates that the waste stream data will be accepted when reported.
 */
@Service
@Transactional(readOnly = true)
class ValidateWasteStream(
  private val wasteStreams: WasteStreams,
  private val wasteStreamValidator: WasteStreamValidator
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  /**
   * Validates a waste stream by its number.
   *
   * @param command The validation command containing the waste stream number
   * @return The validation result
   * @throws IllegalArgumentException if the waste stream does not exist
   */
  fun handle(command: ValidateWasteStreamCommand): WasteStreamValidationResult {
    logger.info("Validating waste stream: ${command.wasteStreamNumber.number}")

    val wasteStream = wasteStreams.findByNumber(command.wasteStreamNumber)
      ?: throw IllegalArgumentException("Afvalstroom met nummer ${command.wasteStreamNumber.number} niet gevonden")

    return wasteStreamValidator.validate(wasteStream)
  }
}

data class ValidateWasteStreamCommand(
  val wasteStreamNumber: WasteStreamNumber
)
