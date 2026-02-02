package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

interface CreateWeightTicketFromWasteStream {
  fun execute(cmd: CreateWeightTicketFromWasteStreamCommand): CreateWeightTicketFromWasteStreamResult
}

data class CreateWeightTicketFromWasteStreamCommand(
  val wasteStreamNumber: String,
)

data class CreateWeightTicketFromWasteStreamResult(
  val weightTicketId: Long,
)

@Service
class CreateWeightTicketFromWasteStreamService(
  private val wasteStreams: WasteStreams,
  private val weightTickets: WeightTickets,
) : CreateWeightTicketFromWasteStream {

  @Transactional
  override fun execute(cmd: CreateWeightTicketFromWasteStreamCommand): CreateWeightTicketFromWasteStreamResult {
    val wasteStream = wasteStreams.findByNumber(WasteStreamNumber(cmd.wasteStreamNumber))
      ?: throw EntityNotFoundException("Afvalstroom met nummer ${cmd.wasteStreamNumber} niet gevonden")

    val catalogItemId = wasteStream.catalogItemId
      ?: throw IllegalStateException("Afvalstroom ${cmd.wasteStreamNumber} heeft geen gekoppeld catalogusitem")

    val line = WeightTicketLine(
      waste = wasteStream.wasteStreamNumber,
      catalogItemId = catalogItemId,
      catalogItemType = CatalogItemType.WASTE_STREAM,
      weight = Weight(BigDecimal.ZERO, Weight.WeightUnit.KILOGRAM),
    )

    val id = weightTickets.nextId()

    val weightTicket = WeightTicket(
      id = id,
      consignorParty = wasteStream.consignorParty,
      lines = WeightTicketLines(listOf(line)),
      productLines = WeightTicketProductLines(emptyList()),
      secondWeighing = null,
      tarraWeight = null,
      weightedAt = null,
      carrierParty = null,
      direction = WeightTicketDirection.INBOUND,
      pickupLocation = wasteStream.pickupLocation,
      deliveryLocation = null,
      truckLicensePlate = null,
      reclamation = null,
      note = null,
      status = WeightTicketStatus.DRAFT,
    )

    weightTickets.save(weightTicket)

    return CreateWeightTicketFromWasteStreamResult(weightTicketId = id.number)
  }
}
