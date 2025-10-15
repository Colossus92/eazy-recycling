package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketDetailView
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketListView
import nl.eazysoftware.eazyrecyclingservice.application.usecase.CreateWeightTicket
import nl.eazysoftware.eazyrecyclingservice.application.usecase.CreateWeightTicketCommand
import nl.eazysoftware.eazyrecyclingservice.application.usecase.DeleteWeightTicket
import nl.eazysoftware.eazyrecyclingservice.application.usecase.DeleteWeightTicketCommand
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.service.WeightTicketService
import nl.eazysoftware.eazyrecyclingservice.domain.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.weightticket.WeightTicketId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/weight-tickets")
class WeightTicketController(
  private val create: CreateWeightTicket,
  private val weightTicketService: WeightTicketService,
  private val deleteWeightTicket: DeleteWeightTicket,
) {
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun create(@RequestBody body: CreateWeightTicketRequest): CreateWeightTicketResponse {
    val result = create.handle(body.toCommand())
    return CreateWeightTicketResponse(id = result.id.number)
  }

  @GetMapping
  fun getWeightTickets(): List<WeightTicketListView> {
    return weightTicketService.getAllWeightTickets()
  }

  @GetMapping("/{weightTicketId}")
  fun getWeightTicketByNumber(
    @PathVariable
    weightTicketId: Long
  ): WeightTicketDetailView {
    return weightTicketService.getWeightTicketByNumber(WeightTicketId(weightTicketId))
  }

  @DeleteMapping("/{weightTicketId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun delete(
    @PathVariable
    weightTicketId: Long
  ) {
    deleteWeightTicket.handle(DeleteWeightTicketCommand(WeightTicketId(weightTicketId)))
  }
}

data class CreateWeightTicketRequest(
  val carrierParty: UUID,
  val consignorParty: ConsignorRequest,
  val truckLicensePlate: String,
  val reclamation: String,
  val note: String?,
) {
  fun toCommand(): CreateWeightTicketCommand {
    // map ids to value objects here
    return CreateWeightTicketCommand(
      carrierParty = CompanyId(carrierParty),
      consignorParty = consignorParty.toDomain(),
      truckLicensePlate = LicensePlate(truckLicensePlate),
      reclamation = reclamation,
      note = note?.let { Note(it) },
    )
  }
}

data class CreateWeightTicketResponse(val id: Long)
