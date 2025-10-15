package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import jakarta.validation.constraints.Pattern
import nl.eazysoftware.eazyrecyclingservice.application.usecase.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.waste.Weight
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStreamNumber
import org.hibernate.validator.constraints.Length
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/weight-tickets")
class WeightTicketController(
  private val create: CreateWeightTicket
) {
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun create(@RequestBody body: CreateWeightTicketRequest): CreateWeightTicketResponse {
    val result = create.handle(body.toCommand())
    return CreateWeightTicketResponse(id = result.id.number)
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

data class CreateWeightTicketResponse(val id: Int)
