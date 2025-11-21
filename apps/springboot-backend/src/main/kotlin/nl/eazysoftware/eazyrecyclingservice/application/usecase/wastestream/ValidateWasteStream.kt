package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream

import nl.eazysoftware.eazyrecyclingservice.domain.model.Tenant
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ValidationError
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreamValidationResult
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreamValidator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Use case for validating a waste stream against the external Amice validation service.
 * This validates that the waste stream data will be accepted when reported.
 *
 * Only active when amice.enabled=true
 */
@Service
@Transactional(readOnly = true)
class ValidateWasteStream(
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
    logger.info("Validating waste stream: ${command.wasteStream.wasteStreamNumber.number}")

    if (command.wasteStream.deliveryLocation.processorPartyId != Tenant.processorPartyId) {
      return WasteStreamValidationResult(
        wasteStreamNumber = command.wasteStream.wasteStreamNumber.number,
        isValid = true,
        errors = emptyList(),
        requestData = null
      )
    }

    return wasteStreamValidator.validate(command.wasteStream)
  }
}

data class ValidateWasteStreamCommand(
  val wasteStream: WasteStream
)
