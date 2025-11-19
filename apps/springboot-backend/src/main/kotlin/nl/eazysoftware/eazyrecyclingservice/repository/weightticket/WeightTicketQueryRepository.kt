package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.application.query.*
import nl.eazysoftware.eazyrecyclingservice.config.clock.toDisplayTime
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketStatus
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationMapper
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyViewMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import kotlin.time.toKotlinInstant

@Repository
class WeightTicketQueryRepository(
  private val entityManager: EntityManager,
  private val jpaRepository: WeightTicketJpaRepository,
  private val pickupLocationMapper: PickupLocationMapper
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
        note = columns[3] as String?,
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
      secondWeighingValue = weightTicket.secondWeighingValue,
      secondWeighingUnit = weightTicket.secondWeighingUnit.toString(),
      tarraWeightValue = weightTicket.tarraWeightValue,
      tarraWeightUnit = weightTicket.tarraWeightUnit.toString(),
      carrierParty = weightTicket.carrierParty?.let { CompanyViewMapper.map(it) },
      direction = weightTicket.direction.name,
      pickupLocation = weightTicket.pickupLocation?.let { pickupLocationMapper.toView(it) },
      deliveryLocation = weightTicket.deliveryLocation?.let { pickupLocationMapper.toView(it) },
      truckLicensePlate = weightTicket.truckLicensePlate,
      reclamation = weightTicket.reclamation,
      note = weightTicket.note,
      cancellationReason = weightTicket.cancellationReason,
      createdAt = weightTicket.createdAt.toKotlinInstant().toDisplayTime(),
      updatedAt = weightTicket.updatedAt?.toKotlinInstant()?.toDisplayTime(),
      weightedAt = weightTicket.weightedAt?.toKotlinInstant()?.toDisplayTime(),
      pdfUrl = weightTicket.pdfUrl,
    )
  }
}
