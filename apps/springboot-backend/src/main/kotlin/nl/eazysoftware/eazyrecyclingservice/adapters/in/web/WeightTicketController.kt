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
  val consignorParty: UUID,
  val consigneeParty: UUID,
  val pickupParty: UUID,
  val truckId: String,
  val goods: List<GoodsRequest>,
  val reclamation: String,
  val note: String?,
  val loadingAddress: AddressRequest,
  val unloadingAddress: AddressRequest
) {
  fun toCommand(): CreateWeightTicketCommand {
    // map ids to value objects here
    return CreateWeightTicketCommand(
      carrierParty = CompanyId(carrierParty),
      consignorParty = CompanyId(consignorParty),
      pickupParty = CompanyId(pickupParty),
      truckId = LicensePlate(truckId),
      goods = goods.map { it.toCommand() },
      reclamation = reclamation,
      note = note?.let { Note(it) },
      loadingAddress = loadingAddress.toDomain(),
      unloadingAddress = unloadingAddress.toDomain()
    )
  }
}

data class CreateWeightTicketResponse(val id: Int)



/**
 * DTO for goods in weight ticket request.
 * Only contains the waste stream number - the full WasteStream will be fetched and validated in the use case.
 */
data class GoodsRequest(
  @field:Length(min = 12, max = 12, message = "Afvalstroomnummer moet exact 12 tekens lang zijn")
  @field:Pattern(regexp = "^[0-9]+$", message = "Afvalstroomnummers mag alleen getallen bevatten")
  val wasteStreamNumber: String,
  val netWeight: Double,
  val unit: String = "Kg"
) {
  fun toCommand() = GoodsCommand(
    wasteStreamNumber = WasteStreamNumber(wasteStreamNumber),
    weight = Weight(
      value = BigDecimal.valueOf(netWeight),
      unit = Weight.WeightUnit.valueOf(unit.uppercase())
    )
  )
}
