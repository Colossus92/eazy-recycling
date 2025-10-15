package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.application.query.GetAllWeightTickets
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketListView
import nl.eazysoftware.eazyrecyclingservice.domain.weightticket.WeightTicketStatus
import org.springframework.stereotype.Repository

@Repository
class WeightTicketQueryRepository(
    private val entityManager: EntityManager,
) : GetAllWeightTickets {

  override fun execute(): List<WeightTicketListView> {
    val query = """
            SELECT
                wt.id,
                c.name,
                wt.status,
                wt.note,
                wt.created_at
            FROM weight_tickets wt
            JOIN companies c ON wt.consignor_party_id = c.id
            ORDER BY wt.id
        """.trimIndent()

    val results = entityManager.createNativeQuery(query).resultList

    return results.map { row ->
      val columns = row as Array<*>
      WeightTicketListView(
        id = columns[0] as Long,
        consignorPartyName = columns[1] as String,
        status = WeightTicketStatus.valueOf(columns[2] as String),
        note = columns[3] as String,
      )
    }
  }
}
