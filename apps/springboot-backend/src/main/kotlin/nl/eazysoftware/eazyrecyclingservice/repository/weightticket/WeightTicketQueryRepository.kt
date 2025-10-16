package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import jakarta.persistence.EntityManager
import kotlinx.datetime.toKotlinInstant
import nl.eazysoftware.eazyrecyclingservice.application.query.ConsignorView
import nl.eazysoftware.eazyrecyclingservice.application.query.GetAllWeightTickets
import nl.eazysoftware.eazyrecyclingservice.application.query.GetWeightTicketByNumber
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketDetailView
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketLineView
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketListView
import nl.eazysoftware.eazyrecyclingservice.config.clock.toDisplayTime
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketStatus
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyViewMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class WeightTicketQueryRepository(
    private val entityManager: EntityManager,
    private val jpaRepository: WeightTicketJpaRepository,
) : GetAllWeightTickets, GetWeightTicketByNumber {

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

  override fun execute(weightTicketid: WeightTicketId): WeightTicketDetailView? {
    val weightTicket = jpaRepository.findByIdOrNull(weightTicketid.number) ?: return null

    return WeightTicketDetailView(
      id = weightTicket.id,
      consignorParty = ConsignorView.CompanyConsignorView(CompanyViewMapper.map(weightTicket.consignorParty)),
      status = weightTicket.status.name,
      lines = weightTicket.lines.map { line ->
        WeightTicketLineView(
          wasteStreamNumber = line.wasteStreamNumber,
          weightValue = line.weightValue,
          weightUnit = line.weightUnit.name,
        )
      },
      carrierParty = weightTicket.carrierParty?.let { CompanyViewMapper.map(it) },
      truckLicensePlate = weightTicket.truckLicensePlate,
      reclamation = weightTicket.reclamation,
      note = weightTicket.note,
      createdAt = weightTicket.createdAt.toKotlinInstant().toDisplayTime(),
      updatedAt = weightTicket.updatedAt?.toKotlinInstant()?.toDisplayTime(),
      weightedAt = weightTicket.weightedAt?.toKotlinInstant()?.toDisplayTime(),
    )
  }
}
