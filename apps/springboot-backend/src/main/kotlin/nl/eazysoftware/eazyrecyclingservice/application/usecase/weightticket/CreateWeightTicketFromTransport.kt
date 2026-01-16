package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteTransports
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

interface CreateWeightTicketFromTransport {
  fun execute(cmd: CreateWeightTicketFromTransportCommand): CreateWeightTicketFromTransportResult
}

data class CreateWeightTicketFromTransportCommand(
  val transportId: UUID,
)

data class CreateWeightTicketFromTransportResult(
  val weightTicketId: Long,
)

@Service
class CreateWeightTicketFromTransportService(
  private val wasteTransports: WasteTransports,
  private val wasteStreams: WasteStreams,
  private val weightTickets: WeightTickets,
) : CreateWeightTicketFromTransport {

  @Transactional
  override fun execute(cmd: CreateWeightTicketFromTransportCommand): CreateWeightTicketFromTransportResult {
    val transport = wasteTransports.findById(TransportId(cmd.transportId))
      ?: throw EntityNotFoundException("Transport met id ${cmd.transportId} niet gevonden")

    // Get the first waste stream to determine the consignor
    val firstGoods = transport.goods.firstOrNull()
      ?: throw IllegalStateException("Transport moet minimaal één afvalstroom bevatten om een weegbon aan te maken")

    val firstWasteStream = wasteStreams.findByNumber(firstGoods.wasteStreamNumber)
      ?: throw EntityNotFoundException("Afvalstroom met nummer ${firstGoods.wasteStreamNumber.number} niet gevonden")

    // Map goods items to weight ticket lines
    val lines = transport.goods.map { goods ->
      val wasteStream = wasteStreams.findByNumber(goods.wasteStreamNumber)
        ?: throw EntityNotFoundException("Afvalstroom met nummer ${goods.wasteStreamNumber.number} niet gevonden")

      val catalogItemId = wasteStream.catalogItemId
        ?: throw IllegalStateException("Afvalstroom ${goods.wasteStreamNumber.number} heeft geen gekoppeld catalogusitem")

      WeightTicketLine(
        waste = goods.wasteStreamNumber,
        catalogItemId = catalogItemId,
        catalogItemType = CatalogItemType.WASTE_STREAM,
        weight = Weight(BigDecimal.valueOf(goods.netNetWeight), Weight.WeightUnit.KILOGRAM),
      )
    }

    val id = weightTickets.nextId()

    val weightTicket = WeightTicket(
      id = id,
      consignorParty = firstWasteStream.consignorParty,
      lines = WeightTicketLines(lines),
      productLines = WeightTicketProductLines(emptyList()),
      secondWeighing = null,
      tarraWeight = null,
      weightedAt = null,
      carrierParty = transport.carrierParty,
      direction = WeightTicketDirection.INBOUND,
      pickupLocation = null,
      deliveryLocation = null,
      truckLicensePlate = transport.truck,
      reclamation = null,
      note = transport.note,
      status = WeightTicketStatus.DRAFT,

    )

    weightTickets.save(weightTicket)
    transport.weightTicketId = weightTicket.id
    wasteTransports.save(transport)

    return CreateWeightTicketFromTransportResult(weightTicketId = id.number)
  }
}
