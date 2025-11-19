package nl.eazysoftware.eazyrecyclingservice.application.query

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Query to retrieve the weight ticket PDF URL by transport ID.
 * 
 * This query follows hexagonal architecture principles:
 * - Depends on domain port rather than infrastructure
 * - Returns view models suitable for presentation layer
 * - Read-only operation with @Transactional(readOnly = true)
 * - Joins transports and weight_tickets tables to fetch the PDF URL
 */
interface GetWeightTicketPdfUrlByTransport {
  fun execute(transportId: UUID): WeightTicketPdfUrlView?
}

@Repository
@Transactional(readOnly = true)
class GetWeightTicketPdfUrlByTransportImpl(
  private val entityManager: EntityManager
) : GetWeightTicketPdfUrlByTransport {

  override fun execute(transportId: UUID): WeightTicketPdfUrlView? {
    val query = """
      SELECT wt.pdf_url
      FROM transports t
      LEFT JOIN weight_tickets wt ON t.weight_ticket_id = wt.id
      WHERE t.id = :transportId
    """.trimIndent()

    val result = entityManager.createNativeQuery(query)
      .setParameter("transportId", transportId)
      .resultList
      .firstOrNull()

    return if (result != null) {
      val pdfUrl = result as? String
      if (pdfUrl.isNullOrBlank()) {
        null
      } else {
        WeightTicketPdfUrlView(pdfUrl = pdfUrl)
      }
    } else {
      null
    }
  }
}

/**
 * View model representing the PDF URL of a weight ticket.
 * Returns empty/null if no PDF URL is available.
 */
data class WeightTicketPdfUrlView(
  val pdfUrl: String
)
