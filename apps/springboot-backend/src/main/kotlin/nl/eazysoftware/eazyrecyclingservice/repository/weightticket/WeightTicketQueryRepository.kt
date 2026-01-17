package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.application.query.*
import nl.eazysoftware.eazyrecyclingservice.config.clock.toDisplayTime
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketStatus
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationMapper
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyViewMapper
import nl.eazysoftware.eazyrecyclingservice.repository.TransportRepository
import org.springframework.stereotype.Repository
import kotlin.time.toKotlinInstant

@Repository
class WeightTicketQueryRepository(
  private val entityManager: EntityManager,
  private val jpaRepository: WeightTicketJpaRepository,
  private val pickupLocationMapper: PickupLocationMapper,
  private val transportRepository: TransportRepository
) : GetAllWeightTickets, GetWeightTicketByNumber {

  override fun execute(): List<WeightTicketListView> {
    val query = """
            SELECT
                wt.number,
                c.name,
                (SELECT SUM(wtl.weight_value)
                 FROM weight_ticket_lines wtl
                 WHERE wtl.weight_ticket_id = wt.id) as total_weight,
                wt.weighted_at,
                wt.note,
                wt.status,
                wt.created_at
            FROM weight_tickets wt
            JOIN companies c ON wt.consignor_party_id = c.id
            ORDER BY wt.number
        """.trimIndent()

    val results = entityManager.createNativeQuery(query).resultList

    return results.map { row ->
      val columns = row as Array<*>
      WeightTicketListView(
        id = columns[0] as Long,
        consignorPartyName = columns[1] as String,
        totalWeight = (columns[2] as? Number)?.toDouble(),
        weighingDate = columns[3] as? java.time.Instant,
        note = columns[4] as String?,
        status = WeightTicketStatus.valueOf(columns[5] as String),
      )
    }
  }

  override fun execute(weightTicketNumber: Long): WeightTicketDetailView? {
    val weightTicket = jpaRepository.findByNumber(weightTicketNumber) ?: return null

    val linkedTransport = transportRepository.findByWeightTicketNumber(weightTicketNumber).firstOrNull()

    return WeightTicketDetailView(
      id = weightTicket.number,
      consignorParty = ConsignorView.CompanyConsignorView(CompanyViewMapper.map(weightTicket.consignorParty)),
      status = weightTicket.status.name,
      lines = weightTicket.lines.map { line ->
        WeightTicketLineView(
          wasteStreamNumber = line.wasteStreamNumber,
          catalogItemId = line.catalogItemId,
          itemName = line.catalogItem.name,
          weightValue = line.weightValue,
          weightUnit = line.weightUnit.name,
        )
      },
      productLines = weightTicket.productLines.map { line ->
        WeightTicketProductLineView(
          catalogItemId = line.catalogItemId,
          itemName = line.catalogItem.name,
          quantity = line.quantity,
          unit = line.unit,
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
      linkedInvoiceId = weightTicket.linkedInvoiceId,
      linkedTransportId = linkedTransport?.id,
      createdAt = weightTicket.createdAt?.toKotlinInstant()?.toDisplayTime(),
      createdByName = weightTicket.createdBy,
      updatedAt = weightTicket.updatedAt?.toKotlinInstant()?.toDisplayTime(),
      updatedByName = weightTicket.updatedBy,
      weightedAt = weightTicket.weightedAt?.toKotlinInstant()?.toDisplayTime(),
      pdfUrl = weightTicket.pdfUrl,
    )
  }
}
