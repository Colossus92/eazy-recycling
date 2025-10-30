package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import kotlinx.datetime.toKotlinInstant
import nl.eazysoftware.eazyrecyclingservice.application.query.ConsignorView
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketDetailView
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketLineView
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketListView
import nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket.*
import nl.eazysoftware.eazyrecyclingservice.config.clock.toDisplayTime
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.*
import nl.eazysoftware.eazyrecyclingservice.domain.service.WeightTicketService
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyViewMapper
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.*
import kotlin.collections.map

@PreAuthorize(HAS_ANY_ROLE)
@RestController
@RequestMapping("/weight-tickets")
class WeightTicketController(
  private val create: CreateWeightTicket,
  private val weightTicketService: WeightTicketService,
  private val updateWeightTicket: UpdateWeightTicket,
  private val cancelWeightTicket: CancelWeightTicket,
  private val splitWeightTicket: SplitWeightTicket,
  private val completeWeightTicket: CompleteWeightTicket,
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

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
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

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @PostMapping("/{weightTicketId}/split")
  @ResponseStatus(HttpStatus.OK)
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

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @PostMapping("/{weightTicketId}/complete")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun complete(
    @PathVariable
    weightTicketId: Long,
  ) {
    completeWeightTicket.handle(
      CompleteWeightTicketCommand(
        WeightTicketId(weightTicketId)
      )
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
