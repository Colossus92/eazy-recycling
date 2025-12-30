package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import jakarta.persistence.EntityNotFoundException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

interface CompleteWeightTicket {
  fun handle(cmd: CompleteWeightTicketCommand)
}

data class CompleteWeightTicketCommand(
  val weightTicketNumber: Long,
)

@Service
class CompleteWeightTicketService(
  private val weightTickets: WeightTickets,
  private val supabaseClient: SupabaseClient,
) : CompleteWeightTicket {

  private val logger = LoggerFactory.getLogger(javaClass)
  private val coroutineScope = CoroutineScope(Dispatchers.IO)

  override fun handle(cmd: CompleteWeightTicketCommand) {
    val weightTicket = weightTickets.findByNumber(cmd.weightTicketNumber)
      ?: throw EntityNotFoundException("Weegbon met nummer ${cmd.weightTicketNumber} bestaat niet")

    weightTicket.complete()
    weightTickets.save(weightTicket)

    // Trigger PDF generation asynchronously
    triggerPdfGeneration(cmd.weightTicketNumber)
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
