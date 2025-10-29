package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketDetailView
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketListView
import nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.CancellationReason
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketLine
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketLines
import nl.eazysoftware.eazyrecyclingservice.domain.service.WeightTicketService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.*

@RestController
@RequestMapping("/weight-tickets")
class WeightTicketController(
  private val create: CreateWeightTicket,
  private val weightTicketService: WeightTicketService,
  private val updateWeightTicket: UpdateWeightTicket,
  private val cancelWeightTicket: CancelWeightTicket,
  private val splitWeightTicket: SplitWeightTicket,
) {
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun create(@RequestBody body: WeightTicketRequest): CreateWeightTicketResponse {
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

  @PutMapping("/{weightTicketId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun update(
    @PathVariable
    weightTicketId: Long,
    @Valid @RequestBody request: WeightTicketRequest
  ) {
    updateWeightTicket.handle(WeightTicketId(weightTicketId), request.toCommand())
  }

  @PostMapping("/{weightTicketId}/cancel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun cancel(
    @PathVariable
    weightTicketId: Long,
    @Valid @RequestBody request: CancelWeightTicketRequest
  ) {
    cancelWeightTicket.handle(
      CancelWeightTicketCommand(
        WeightTicketId(weightTicketId),
        CancellationReason(request.cancellationReason),
      )
    )
  }

  @PostMapping("/{weightTicketId}/split")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun split(
    @PathVariable
    weightTicketId: Long,
    @Valid @RequestBody request: SplitWeightTicketRequest
  ): SplitWeightTicketResponse {
    val newTicketId = splitWeightTicket.handle(
      SplitWeightTicketCommand(
        WeightTicketId(weightTicketId),
        request.originalWeightTicketPercentage,
        request.newWeightTicketPercentage,
      )
    )

    return SplitWeightTicketResponse(
      originalWeightTicketId = weightTicketId,
      newWeightTicketId = newTicketId.number,
    )
  }
}

data class SplitWeightTicketRequest(
  @field:Min(value = 1, message = "De minimale waarde bij splitsen is 1%")
  @field:Max(value = 100, message = "De maximale waarde bij splitsen is 99%")
  val originalWeightTicketPercentage: Int,

  @field:Min(value = 1, message = "De minimale waarde bij splitsen is 1%")
  @field:Max(value = 100, message = "De maximale waarde bij splitsen is 99%")
  val newWeightTicketPercentage: Int,
)

data class SplitWeightTicketResponse(
  val originalWeightTicketId: Long,
  val newWeightTicketId: Long,
)

data class CancelWeightTicketRequest(
  @field:NotBlank(message = "Een reden van annulering is verplicht")
  val cancellationReason: String,
)

data class WeightTicketRequest(
  val consignorParty: ConsignorRequest,
  val lines: List<WeightTicketLineRequest>,
  val carrierParty: UUID?,
  val truckLicensePlate: String?,
  val reclamation: String?,
  val note: String?,
) {
  fun toCommand(): WeightTicketCommand {
    return WeightTicketCommand(
      lines = lines.toDomain(),
      consignorParty = consignorParty.toDomain(),
      carrierParty = carrierParty?.let { CompanyId(it) },
      truckLicensePlate = truckLicensePlate?.let { LicensePlate(it) },
      reclamation = reclamation,
      note = note?.let { Note(it) },
    )
  }
}

data class CreateWeightTicketResponse(val id: Long)

data class WeightTicketLineRequest(
  val wasteStreamNumber: String,
  val weight: WeightRequest,
)

data class WeightRequest(
  val value: String,
  val unit: WeightUnitRequest,
)

enum class WeightUnitRequest {
  KG,
}

fun List<WeightTicketLineRequest>.toDomain(): WeightTicketLines {
  return WeightTicketLines(
    this.map { line ->
      WeightTicketLine(
        waste = WasteStreamNumber(line.wasteStreamNumber),
        weight = Weight(
          value = BigDecimal(line.weight.value),
          unit = when (line.weight.unit) {
            WeightUnitRequest.KG -> Weight.WeightUnit.KILOGRAM
          }
        )
      )
    }
  )
}
