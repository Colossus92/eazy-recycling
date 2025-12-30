package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.application.query.GetWeightTicketsByWasteStream
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketsByWasteStreamView
import nl.eazysoftware.eazyrecyclingservice.config.clock.toDisplayTime
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import kotlin.time.toKotlinInstant

/**
 * Query adapter for retrieving weight tickets by waste stream number.
 * Uses native SQL to directly map query results to the view.
 */
@Repository
class WeightTicketsByWasteStreamQueryAdapter(
    private val entityManager: EntityManager
) : GetWeightTicketsByWasteStream {

    override fun execute(wasteStreamNumber: WasteStreamNumber): List<WeightTicketsByWasteStreamView> {
        val query = """
            SELECT
                wt.number as weight_ticket_number,
                wt.weighted_at,
                wtl.weight_value as amount,
                wt.created_by
            FROM weight_tickets wt
            JOIN weight_ticket_lines wtl ON wt.id = wtl.weight_ticket_id
            WHERE wtl.waste_stream_number = :wasteStreamNumber
            ORDER BY wt.weighted_at DESC NULLS LAST, wt.number DESC
        """.trimIndent()

        @Suppress("UNCHECKED_CAST")
        val results = entityManager.createNativeQuery(query, WeightTicketsByWasteStreamResult::class.java)
            .setParameter("wasteStreamNumber", wasteStreamNumber.number)
            .resultList as List<WeightTicketsByWasteStreamResult>

        return results.map { row ->
            WeightTicketsByWasteStreamView(
                weightTicketNumber = row.weightTicketNumber,
                weightedAt = row.weightedAt?.toKotlinInstant()?.toDisplayTime(),
                amount = row.amount,
                createdBy = row.createdBy
            )
        }
    }

  data class WeightTicketsByWasteStreamResult(
    val weightTicketNumber: Long,
    val weightedAt: Instant?,
    val amount: BigDecimal,
    val createdBy: String?
  )
}
