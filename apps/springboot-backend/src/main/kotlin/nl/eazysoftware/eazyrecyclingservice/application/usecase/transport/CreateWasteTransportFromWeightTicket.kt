package nl.eazysoftware.eazyrecyclingservice.application.usecase.transport

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.GoodsItem
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.toKotlinInstant


interface CreateWasteTransportFromWeightTicket {
  fun execute(cmd: CreateWasteTransportFromWeightTicketCommand): CreateWasteTransportResult
}

/**
 * Command for creating a waste transport from a weight ticket.
 * Includes optional pickup and delivery datetime overrides.
 */
data class CreateWasteTransportFromWeightTicketCommand(
  val weightTicketId: WeightTicketId,
  val pickupDateTime: LocalDateTime,
  val deliveryDateTime: LocalDateTime?,
)

@Service
class CreateWasteTransportFromWeightTicketService(
  private val weightTickets: WeightTickets,
  private val createWasteTransport: CreateWasteTransport,
) : CreateWasteTransportFromWeightTicket {

  @Transactional
  override fun execute(cmd: CreateWasteTransportFromWeightTicketCommand): CreateWasteTransportResult {
    val weightTicket = weightTickets.findById(cmd.weightTicketId)
      ?: throw EntityNotFoundException("Weegbon met nummer ${cmd.weightTicketId.number} niet gevonden")
    val carrierParty = weightTicket.carrierParty ?: throw IllegalStateException("Weegbon moet een vervoerder hebben om een afvaltransport aan te maken")

    // Map WeightTicket lines to GoodsItems
    val goods = weightTicket.lines.getLines()
      .filter { it.waste != null }
      .map { line ->
      GoodsItem(
        wasteStreamNumber = line.waste!!,
        netNetWeight = line.weight.value.toDouble(),
        unit = line.weight.unit.name,
        quantity = 1
      )
    }

    // Convert LocalDateTime to Instant using CET timezone
    val pickupInstant = cmd.pickupDateTime.atZone(ZoneId.of("Europe/Amsterdam")).toInstant().toKotlinInstant()
    val deliveryInstant = cmd.deliveryDateTime?.atZone(ZoneId.of("Europe/Amsterdam"))?.toInstant()?.toKotlinInstant()

    // Create the WasteTransport using the existing use case
    val createCommand = CreateWasteTransportCommand(
      carrierParty = carrierParty,
      pickupDateTime = pickupInstant,
      deliveryDateTime = deliveryInstant,
      transportType = TransportType.WASTE,
      goods = goods,
      wasteContainer = null,
      containerOperation = null,
      truck = weightTicket.truckLicensePlate,
      driver = null,
      note = weightTicket.note,
      weightTicketId = cmd.weightTicketId,
    )

    return createWasteTransport.handle(createCommand)
  }
}
