package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.EersteOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.MaandelijkseOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.AmiceSessions
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclarations
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.LmaDeclarationDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Use case for approving and submitting corrective declarations to LMA.
 *
 * When a corrective declaration is approved:
 * 1. The declaration status is changed from CORRECTIVE to PENDING
 * 2. The declaration is submitted to LMA via the Amice SOAP service
 * 3. Upon successful submission, the status is updated based on the response
 */
interface ApproveCorrectiveDeclaration {
  fun approve(declarationId: String): ApprovalResult
}

data class ApprovalResult(
  val success: Boolean,
  val message: String,
  val declarationId: String,
)

@Service
class ApproveCorrectiveDeclarationService(
  private val lmaDeclarations: LmaDeclarations,
  private val amiceSessions: AmiceSessions,
) : ApproveCorrectiveDeclaration {

  private val logger = LoggerFactory.getLogger(javaClass)

  @Transactional
  override fun approve(declarationId: String): ApprovalResult {
    logger.info("Approving corrective declaration: {}", declarationId)

    // Find the declaration
    val declaration = lmaDeclarations.findById(declarationId)
      ?: return ApprovalResult(
        success = false,
        message = "Declaration not found: $declarationId",
        declarationId = declarationId,
      )

    // Verify it's a corrective declaration
    if (declaration.status != LmaDeclarationDto.Status.WAITING_APPROVAL) {
      return ApprovalResult(
        success = false,
        message = "Declaration is not in CORRECTIVE status: ${declaration.status}",
        declarationId = declarationId,
      )
    }

    // Determine if this is a first receival or monthly receival based on the ID prefix
    val isFirstReceival = declarationId.startsWith("LATE-FIRST-")

    try {
      if (isFirstReceival) {
        // Submit as first receival
        val soapMessage = mapToFirstReceivalSoapMessage(declaration)
        amiceSessions.declareFirstReceivals(listOf(soapMessage))
      } else {
        // Submit as monthly receival
        val soapMessage = mapToMonthlyReceivalSoapMessage(declaration)
        amiceSessions.declareMonthlyReceivals(listOf(soapMessage))
      }

      // Update status to PENDING (will be updated to COMPLETED when session result is received)
      val updatedDeclaration = declaration.copy(status = LmaDeclarationDto.Status.PENDING)
      lmaDeclarations.saveAll(listOf(updatedDeclaration))

      logger.info("Successfully submitted corrective declaration: {}", declarationId)

      return ApprovalResult(
        success = true,
        message = "Declaration approved and submitted to LMA",
        declarationId = declarationId,
      )
    } catch (e: Exception) {
      logger.error("Failed to submit corrective declaration: {}", declarationId, e)

      // Update status to FAILED
      val failedDeclaration = declaration.copy(
        status = LmaDeclarationDto.Status.FAILED,
        errors = listOf(e.message ?: "Unknown error"),
      )
      lmaDeclarations.saveAll(listOf(failedDeclaration))

      return ApprovalResult(
        success = false,
        message = "Failed to submit declaration: ${e.message}",
        declarationId = declarationId,
      )
    }
  }

  private fun mapToFirstReceivalSoapMessage(declaration: LmaDeclarationDto): EersteOntvangstMeldingDetails {
    val message = EersteOntvangstMeldingDetails()
    message.meldingsNummerMelder = declaration.id
    message.afvalstroomNummer = declaration.wasteStreamNumber
    message.periodeMelding = declaration.period
    message.vervoerders = declaration.transporters.joinToString(",")
    message.totaalGewicht = declaration.totalWeight.toInt()
    message.aantalVrachten = declaration.totalShipments.toShort()
    // Note: Other fields like consignor, pickup location, etc. would need to be fetched
    // from the waste stream configuration. For now, we're sending minimal data.
    return message
  }

  private fun mapToMonthlyReceivalSoapMessage(declaration: LmaDeclarationDto): MaandelijkseOntvangstMeldingDetails {
    val message = MaandelijkseOntvangstMeldingDetails()
    message.meldingsNummerMelder = declaration.id
    message.afvalstroomNummer = declaration.wasteStreamNumber
    message.periodeMelding = declaration.period
    message.vervoerders = declaration.transporters.joinToString(",")
    message.totaalGewicht = declaration.totalWeight.toInt()
    message.aantalVrachten = declaration.totalShipments.toShort()
    return message
  }
}
