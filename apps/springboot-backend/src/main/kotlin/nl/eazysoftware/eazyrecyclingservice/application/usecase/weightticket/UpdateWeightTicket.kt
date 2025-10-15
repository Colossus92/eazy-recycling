package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import nl.eazysoftware.eazyrecyclingservice.domain.weightticket.WeightTicketId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface UpdateWeightTicket {
    fun handle(weightTicketId: WeightTicketId, cmd: WeightTicketCommand)
}

@Service
class UpdateWeightTicketService(
    private val weightTickets: WeightTickets,
) : UpdateWeightTicket {

    @Transactional
    override fun handle(weightTicketId: WeightTicketId, cmd: WeightTicketCommand) {
        val weightTicket = weightTickets.findById(weightTicketId)
            ?: throw EntityNotFoundException("Weegbon met nummer ${weightTicketId.number} bestaat niet")

        weightTicket.update(
            carrierParty = cmd.carrierParty,
            consignorParty = cmd.consignorParty,
            truckLicensePlate = cmd.truckLicensePlate,
            reclamation = cmd.reclamation,
            note = cmd.note,
        )

      weightTickets.save(weightTicket)
    }
}
