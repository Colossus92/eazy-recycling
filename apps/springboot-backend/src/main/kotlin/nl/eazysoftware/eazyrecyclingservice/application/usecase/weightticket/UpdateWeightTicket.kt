package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream.toDomain
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface UpdateWeightTicket {
  fun handle(weightTicketId: WeightTicketId, cmd: WeightTicketCommand)
}

@Service
class UpdateWeightTicketService(
  private val weightTickets: WeightTickets,
  private val projectLocations: ProjectLocations,
  private val companies: Companies,
) : UpdateWeightTicket {

  @Transactional
  override fun handle(weightTicketId: WeightTicketId, cmd: WeightTicketCommand) {
    val weightTicket = weightTickets.findById(weightTicketId)
      ?: throw EntityNotFoundException("Weegbon met nummer ${weightTicketId.number} bestaat niet")

    weightTicket.update(
      lines = cmd.lines,
      tarraWeight = cmd.tarraWeight,
      carrierParty = cmd.carrierParty,
      consignorParty = cmd.consignorParty,
      direction = cmd.direction,
      pickupLocation = cmd.pickupLocation?.toDomain(companies, projectLocations),
      deliveryLocation = cmd.deliveryLocation?.toDomain(companies, projectLocations),
      truckLicensePlate = cmd.truckLicensePlate,
      reclamation = cmd.reclamation,
      note = cmd.note,
    )

    weightTickets.save(weightTicket)
  }
}
