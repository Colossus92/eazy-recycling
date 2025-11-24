package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream.toDomain
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicket
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketStatus
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface CreateCompletedWeightTicket {
  fun handle(cmd: WeightTicketCommand): WeightTicketResult
}

@Service
class CreateCompletedWeightTicketService(
  private val weightTickets: WeightTickets,
  private val projectLocations: ProjectLocations,
  private val companies: Companies,
  private val supabaseClient: SupabaseClient,
) : CreateCompletedWeightTicket {

  private val logger = LoggerFactory.getLogger(javaClass)
  private val coroutineScope = CoroutineScope(Dispatchers.IO)

  @Transactional
  override fun handle(cmd: WeightTicketCommand): WeightTicketResult {
    val id = weightTickets.nextId()

    val ticket = WeightTicket(
      id = id,
      consignorParty = cmd.consignorParty,
      lines = cmd.lines,
      secondWeighing = cmd.secondWeighing,
      tarraWeight = cmd.tarraWeight,
      weightedAt = cmd.weightedAt,
      carrierParty = cmd.carrierParty,
      direction = cmd.direction,
      pickupLocation = cmd.pickupLocation?.toDomain(companies, projectLocations),
      deliveryLocation = cmd.deliveryLocation?.toDomain(companies, projectLocations),
      truckLicensePlate = cmd.truckLicensePlate,
      reclamation = cmd.reclamation,
      note = cmd.note,
      status = WeightTicketStatus.COMPLETED,
    )

    weightTickets.save(ticket)
    
    // Trigger PDF generation asynchronously
    triggerPdfGeneration(id.number)
    
    return WeightTicketResult(id)
  }

  private fun triggerPdfGeneration(weightTicketId: Long) {
    coroutineScope.launch {
      try {
        logger.info("Triggering PDF generation for weight ticket $weightTicketId")

        supabaseClient.functions.invoke(
          function = "weight-ticket-pdf-generator",
          body = mapOf("ticketId" to weightTicketId.toString())
        )

        logger.info("PDF generation triggered successfully for weight ticket $weightTicketId")
      } catch (e: Exception) {
        // Log error but don't fail the completion process
        logger.error("Failed to trigger PDF generation for weight ticket $weightTicketId", e)
      }
    }
  }
}
