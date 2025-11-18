package nl.eazysoftware.eazyrecyclingservice.application.usecase.transport

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.GoodsItem
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Clock


interface CreateWasteTransportFromWeightTicket {

  fun execute(weightTicketId: WeightTicketId) : CreateWasteTransportResult
}

@Service
class CreateWasteTransportFromWeightTicketService(
  private val weightTickets: WeightTickets,
  private val createWasteTransport: CreateWasteTransport,
) : CreateWasteTransportFromWeightTicket {

  @Transactional
  override fun execute(weightTicketId: WeightTicketId): CreateWasteTransportResult {
    val weightTicket = weightTickets.findById(weightTicketId)
      ?: throw EntityNotFoundException("Weegbon met nummer ${weightTicketId.number} niet gevonden")
    val carrierParty = weightTicket.carrierParty ?: throw IllegalStateException("Weegbon moet een vervoerder hebben om een afvaltransport aan te maken")

    // Map WeightTicket lines to GoodsItems
    val goods = weightTicket.lines.getLines().map { line ->
      GoodsItem(
        wasteStreamNumber = line.waste,
        netNetWeight = line.weight.value.toDouble(),
        unit = line.weight.unit.name,
        quantity = 1
      )
    }

    // Create the WasteTransport using the existing use case
    val command = CreateWasteTransportCommand(
      carrierParty = carrierParty,
      pickupDateTime = Clock.System.now(),
      deliveryDateTime = null,
      transportType = TransportType.WASTE,
      goods = goods,
      wasteContainer = null,
      containerOperation = null,
      truck = weightTicket.truckLicensePlate,
      driver = null,
      note = weightTicket.note,
      weightTicketId = weightTicketId,
    )

    return createWasteTransport.handle(command)
  }
}
