package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.FoutDetails
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.OpvragenResultaatVerwerkingMeldingSessieResponseDetails
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.StatusMeldingSessieDetails
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.AmiceSessions
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclarationSessions
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclarations
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.LmaDeclarationDto
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.LmaDeclarationSessionDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
class SessionResultResolver(
    private val lmaDeclarations: LmaDeclarations,
    private val amiceSessions: AmiceSessions,
    private val lmaDeclarationSessions: LmaDeclarationSessions,
) {

  private val logger = LoggerFactory.getLogger(SessionResultResolver::class.java)

  /**
   * Data class to hold common fields extracted from different melding types.
   */
  private data class MeldingResult(
    val meldingsNummerMelder: String,
    val meldingUUID: String?,
    val technischAkkoord: Boolean,
    val meldingFouten: List<FoutDetails>?
  )

  /**
   * Process a single session in its own transaction.
   * This ensures that each session is either fully processed or fully rolled back.
   */
  @Transactional
  fun processSession(session: LmaDeclarationSessionDto) {
    logger.info("Processing session: id={}, type={}", session.id, session.type)

    try {
      // Retrieve session results from AMICE
      val response = amiceSessions.retrieve(session.id)
      val responseDetails = response.opvragenResultaatVerwerkingMeldingSessieResponseDetails

      // Check if there's an error at the response level
      if (responseDetails == null) {
        logger.error("Response details are null for session: id={}", session.id)
        lmaDeclarationSessions.save(session.markFailed(listOf("Response details are null")))
        return
      }

      // Process the response
      val statusMeldingSessie = extractStatusMeldingSessie(responseDetails)

      if (statusMeldingSessie == null) {
        // Check if there's an error in the response
        val foutDetails = extractResponseFoutDetails(responseDetails)

        // Check if session is still being processed
        if (foutDetails.any { it.foutCode == "MeldingSessieNogNietAlleMeldingenVerwerkt" }) {
          logger.info("Session {} is still being processed by AMICE, will retry later", session.id)
          return
        }

        val errors = foutDetails.map { "${it.foutCode}: ${it.foutOmschrijving}" }
        if (errors.isNotEmpty()) {
          logger.error("Session {} has errors: {}", session.id, errors)
          lmaDeclarationSessions.save(session.markFailed(errors))
        } else {
          logger.warn("Session {} has no status and no errors - marking as failed", session.id)
          lmaDeclarationSessions.save(session.markFailed(listOf("No status melding sessie found in response")))
        }
        return
      }

      // Process declarations based on type
      processDeclarations(session, statusMeldingSessie)

    } catch (e: Exception) {
      logger.error("Failed to process session: id={}", session.id, e)
      lmaDeclarationSessions.save(session.markFailed(listOf("Exception: ${e.message}")))
    }
  }

  private fun extractStatusMeldingSessie(responseDetails: OpvragenResultaatVerwerkingMeldingSessieResponseDetails): StatusMeldingSessieDetails? {
    return responseDetails.aanvraagFoutOrStatusMeldingSessie
      .filterIsInstance<StatusMeldingSessieDetails>()
      .firstOrNull()
  }

  private fun extractResponseFoutDetails(responseDetails: OpvragenResultaatVerwerkingMeldingSessieResponseDetails): List<FoutDetails> {
    return responseDetails.aanvraagFoutOrStatusMeldingSessie
      .filterIsInstance<FoutDetails>()
  }

  private fun processDeclarations(session: LmaDeclarationSessionDto, statusMeldingSessie: StatusMeldingSessieDetails) {
    when {
      statusMeldingSessie.eersteOntvangstMeldingen.isNotEmpty() -> {
        logger.info("Processing {} eerste ontvangst melding(en) for session {}", statusMeldingSessie.eersteOntvangstMeldingen.size, session.id)
        processMeldingen(session, statusMeldingSessie.eersteOntvangstMeldingen) { melding ->
          MeldingResult(
            meldingsNummerMelder = melding.meldingsNummerMelder,
            meldingUUID = melding.meldingUUID,
            technischAkkoord = melding.isTechnischAkkoord,
            meldingFouten = melding.meldingFouten
          )
        }
      }
      statusMeldingSessie.maandelijkseOntvangstMeldingen.isNotEmpty() -> {
        logger.info("Processing {} maandelijkse ontvangst melding(en) for session {}", statusMeldingSessie.maandelijkseOntvangstMeldingen.size, session.id)
        processMeldingen(session, statusMeldingSessie.maandelijkseOntvangstMeldingen) { melding ->
          MeldingResult(
            meldingsNummerMelder = melding.meldingsNummerMelder,
            meldingUUID = melding.meldingUUID,
            technischAkkoord = melding.isTechnischAkkoord,
            meldingFouten = melding.meldingFouten
          )
        }
      }
      statusMeldingSessie.afgifteMeldingen.isNotEmpty() -> {
        logger.info("Processing {} afgifte melding(en) for session {}", statusMeldingSessie.afgifteMeldingen.size, session.id)
        processMeldingen(session, statusMeldingSessie.afgifteMeldingen) { melding ->
          MeldingResult(
            meldingsNummerMelder = melding.meldingsNummerMelder,
            meldingUUID = melding.meldingUUID,
            technischAkkoord = melding.isTechnischAkkoord,
            meldingFouten = melding.meldingFouten
          )
        }
      }
      else -> {
        logger.warn("No meldingen found in session {}", session.id)
        lmaDeclarationSessions.save(session.markFailed(listOf("No meldingen found in response")))
      }
    }
  }

  /**
   * Generic method to process any type of melding.
   * Uses a mapper function to extract common fields from different melding types.
   */
  private fun <T> processMeldingen(
      session: LmaDeclarationSessionDto,
      meldingen: List<T>,
      mapper: (T) -> MeldingResult
  ) {
    // Get all declarations for this session
    val declarations = lmaDeclarations.findByIds(session.declarationIds)
    val declarationMap = declarations.associateBy { it.id }

    val updatedDeclarations = mutableListOf<LmaDeclarationDto>()
    val sessionErrors = mutableListOf<String>()

    meldingen.forEach { melding ->
      val result = mapper(melding)
      val declarationId = result.meldingsNummerMelder
      val declaration = declarationMap[declarationId]

      if (declaration == null) {
        logger.warn("Declaration {} not found in database for session {}", declarationId, session.id)
        sessionErrors.add("Declaration $declarationId not found")
        return@forEach
      }

      val errors = extractMeldingErrors(result.meldingFouten)
      val amiceUUID = result.meldingUUID?.let { UUID.fromString(it) }

      val updatedDeclaration = if (errors.isEmpty() && result.technischAkkoord) {
        declaration.copy(
          status = LmaDeclarationDto.Status.COMPLETED,
          amiceUUID = amiceUUID
        )
      } else {
        declaration.copy(
          status = LmaDeclarationDto.Status.FAILED,
          errors = errors,
          amiceUUID = amiceUUID
        )
      }

      updatedDeclarations.add(updatedDeclaration)
    }

    // Save all updated declarations
    lmaDeclarations.saveAll(updatedDeclarations)

    // Update session status
    if (sessionErrors.isEmpty() && updatedDeclarations.all { it.status == LmaDeclarationDto.Status.COMPLETED }) {
      logger.info("Session {} completed successfully", session.id)
      lmaDeclarationSessions.save(session.markCompleted())
    } else {
      logger.error("Session {} failed with errors or failed declarations", session.id)
      val allErrors = sessionErrors + updatedDeclarations.filter { it.status == LmaDeclarationDto.Status.FAILED }
        .flatMap { it.errors ?: emptyList() }
      lmaDeclarationSessions.save(session.markFailed(allErrors))
    }
  }

  private fun extractMeldingErrors(fouten: List<FoutDetails>?): List<String> {
    if (fouten.isNullOrEmpty()) return emptyList()
    return fouten.map { "${it.foutCode}: ${it.foutOmschrijving}" }
  }
}
