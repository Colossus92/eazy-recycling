package nl.eazysoftware.eazyrecyclingservice.application.usecase.transport

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.config.clock.toCetKotlinInstant
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.GoodsItem
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime


interface CreateWasteTransportFromWeightTicket {
  fun execute(cmd: CreateWasteTransportFromWeightTicketCommand): CreateWasteTransportResult
}

/**
 * Command for creating a waste transport from a weight ticket.
 * Includes optional pickup and delivery datetime overrides.
 */
data class CreateWasteTransportFromWeightTicketCommand(
  val weightTicketNumber: Long,
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
    val weightTicket = weightTickets.findByNumber(cmd.weightTicketNumber)
      ?: throw EntityNotFoundException("Weegbon met nummer ${cmd.weightTicketNumber} niet gevonden")
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

    // Convert LocalDateTime to Instant using display timezone (CET/CEST)
    val pickupInstant = cmd.pickupDateTime.toCetKotlinInstant()
    val deliveryInstant = cmd.deliveryDateTime?.toCetKotlinInstant()

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
      weightTicketId = weightTicket.id,
    )

    return createWasteTransport.handle(createCommand)
  }
}
