package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketDetailView
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketListView
import nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket.*
import nl.eazysoftware.eazyrecyclingservice.config.clock.toCetKotlinInstant
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.*
import nl.eazysoftware.eazyrecyclingservice.domain.service.WeightTicketService
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemJpaRepository
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@PreAuthorize(HAS_ANY_ROLE)
@RestController
@RequestMapping("/weight-tickets")
class WeightTicketController(
  private val create: CreateWeightTicket,
  private val createCompleted: CreateCompletedWeightTicket,
  private val weightTicketService: WeightTicketService,
  private val updateWeightTicket: UpdateWeightTicket,
  private val cancelWeightTicket: CancelWeightTicket,
  private val splitWeightTicket: SplitWeightTicket,
  private val copyWeightTicket: CopyWeightTicket,
  private val completeWeightTicket: CompleteWeightTicket,
  private val createInvoiceFromWeightTicket: CreateInvoiceFromWeightTicket,
  private val createWeightTicketFromTransport: CreateWeightTicketFromTransport,
  private val createWeightTicketFromWasteStream: CreateWeightTicketFromWasteStream,
  private val catalogItemRepository: CatalogItemJpaRepository,
) {

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun create(@RequestBody body: WeightTicketRequest): CreateWeightTicketResponse {
    val result = create.handle(body.toCommand(catalogItemRepository))
    return CreateWeightTicketResponse(id = result.id.number)
  }

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @PostMapping("/completed")
  @ResponseStatus(HttpStatus.CREATED)
  fun createCompleted(@RequestBody body: WeightTicketRequest): CreateWeightTicketResponse {
    val result = createCompleted.handle(body.toCommand(catalogItemRepository))
    return CreateWeightTicketResponse(id = result.id.number)
  }

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @GetMapping
  fun getWeightTickets(): List<WeightTicketListView> {
    return weightTicketService.getAllWeightTickets()
  }

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @GetMapping("/{weightTicketNumber}")
  fun getWeightTicketByNumber(
    @PathVariable
    weightTicketNumber: Long
  ): WeightTicketDetailView {
    return weightTicketService.getWeightTicketByNumber(weightTicketNumber)
  }

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @PutMapping("/{weightTicketNumber}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun update(
    @PathVariable
    weightTicketNumber: Long,
    @Valid @RequestBody request: WeightTicketRequest
  ) {
    updateWeightTicket.handle(weightTicketNumber, request.toCommand(catalogItemRepository))
  }

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @PostMapping("/{weightTicketNumber}/cancel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun cancel(
    @PathVariable
    weightTicketNumber: Long,
    @Valid @RequestBody request: CancelWeightTicketRequest
  ) {
    cancelWeightTicket.handle(
      CancelWeightTicketCommand(
        weightTicketNumber,
        CancellationReason(request.cancellationReason),
      )
    )
  }

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @PostMapping("/{weightTicketNumber}/split")
  @ResponseStatus(HttpStatus.OK)
  fun split(
    @PathVariable
    weightTicketNumber: Long,
    @Valid @RequestBody request: SplitWeightTicketRequest
  ): SplitWeightTicketResponse {
    val newTicketId = splitWeightTicket.handle(
      SplitWeightTicketCommand(
        weightTicketNumber,
        request.originalWeightTicketPercentage,
        request.newWeightTicketPercentage,
      )
    )

    return SplitWeightTicketResponse(
      originalWeightTicketId = weightTicketNumber,
      newWeightTicketId = newTicketId.number,
    )
  }

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @PostMapping("/{weightTicketNumber}/complete")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun complete(
    @PathVariable
    weightTicketNumber: Long,
  ) {
    completeWeightTicket.handle(
      CompleteWeightTicketCommand(
        weightTicketNumber
      )
    )
  }

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @PostMapping("/{weightTicketNumber}/copy")
  @ResponseStatus(HttpStatus.OK)
  fun copy(
    @PathVariable
    weightTicketNumber: Long,
  ): CopyWeightTicketResponse {
    val newTicketId = copyWeightTicket.handle(
      CopyWeightTicketCommand(
        weightTicketNumber
      )
    )

    return CopyWeightTicketResponse(
      originalWeightTicketId = weightTicketNumber,
      newWeightTicketId = newTicketId.number,
    )
  }

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @PostMapping("/{weightTicketNumber}/invoice")
  @ResponseStatus(HttpStatus.CREATED)
  fun createInvoice(
    @PathVariable
    weightTicketNumber: Long,
  ): CreateInvoiceFromWeightTicketResult {
    return createInvoiceFromWeightTicket.handle(
      CreateInvoiceFromWeightTicketCommand(
        weightTicketNumber
      )
    )
  }

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @PostMapping("/from-transport")
  @ResponseStatus(HttpStatus.CREATED)
  fun createFromTransport(
    @Valid @RequestBody request: CreateWeightTicketFromTransportRequest
  ): CreateWeightTicketFromTransportResponse {
    val result = createWeightTicketFromTransport.execute(
      CreateWeightTicketFromTransportCommand(
        transportId = request.transportId
      )
    )
    return CreateWeightTicketFromTransportResponse(
      weightTicketId = result.weightTicketId
    )
  }

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @PostMapping("/from-waste-stream")
  @ResponseStatus(HttpStatus.CREATED)
  fun createFromWasteStream(
    @Valid @RequestBody request: CreateWeightTicketFromWasteStreamRequest
  ): CreateWeightTicketFromWasteStreamResponse {
    val result = createWeightTicketFromWasteStream.execute(
      CreateWeightTicketFromWasteStreamCommand(
        wasteStreamNumber = request.wasteStreamNumber
      )
    )
    return CreateWeightTicketFromWasteStreamResponse(
      weightTicketId = result.weightTicketId
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

data class CopyWeightTicketResponse(
  val originalWeightTicketId: Long,
  val newWeightTicketId: Long,
)

data class CancelWeightTicketRequest(
  @field:NotBlank(message = "Een reden van annulering is verplicht")
  val cancellationReason: String,
)

data class CreateWeightTicketFromTransportRequest(
  val transportId: UUID,
)

data class CreateWeightTicketFromTransportResponse(
  val weightTicketId: Long,
)

data class CreateWeightTicketFromWasteStreamRequest(
  val wasteStreamNumber: String,
)

data class CreateWeightTicketFromWasteStreamResponse(
  val weightTicketId: Long,
)

data class WeightTicketRequest(
  val consignorParty: ConsignorRequest,
  val lines: List<WeightTicketLineRequest>,
  val productLines: List<WeightTicketProductLineRequest> = emptyList(),
  val tarraWeightValue: String?,
  val tarraWeightUnit: WeightUnitRequest?,
  val secondWeighingValue: String?,
  val secondWeighingUnit: WeightUnitRequest?,
  val weightedAt: LocalDateTime?,
  val carrierParty: UUID?,
  val direction: WeightTicketDirection,
  val pickupLocation: PickupLocationRequest?,
  val deliveryLocation: PickupLocationRequest?,
  val truckLicensePlate: String?,
  val reclamation: String?,
  val note: String?,
) {
  fun toCommand(catalogItemRepository: CatalogItemJpaRepository): WeightTicketCommand {
    return WeightTicketCommand(
      lines = lines.toDomain(catalogItemRepository),
      productLines = productLines.toDomain(catalogItemRepository),
      secondWeighing = secondWeighingValue?.let { toWeight(it, secondWeighingUnit) },
      tarraWeight = tarraWeightValue?.let { toWeight(it, tarraWeightUnit) },
      weightedAt = weightedAt?.toCetKotlinInstant(),
      consignorParty = consignorParty.toDomain(),
      carrierParty = carrierParty?.let { CompanyId(it) },
      direction = direction,
      pickupLocation = pickupLocation?.toCommand(),
      deliveryLocation = deliveryLocation?.toCommand(),
      truckLicensePlate = truckLicensePlate?.let { LicensePlate(it) },
      reclamation = reclamation,
      note = note?.let { Note(it) },
    )
  }

  private fun toWeight(value: String, unit: WeightUnitRequest?): Weight = Weight(
    BigDecimal(value),
    when (unit) {
      WeightUnitRequest.KG -> Weight.WeightUnit.KILOGRAM
      null -> throw IllegalArgumentException("Eenheid is verplicht bij het opgeven van een gewicht")
    }
  )
}

data class CreateWeightTicketResponse(val id: Long)

data class WeightTicketLineRequest(
  val wasteStreamNumber: String?,
  val weight: WeightRequest,
  val catalogItemId: UUID,
)

data class WeightRequest(
  val value: String,
  val unit: WeightUnitRequest,
) {
  fun toDomain() = Weight(
    value = BigDecimal(value),
    unit = when (unit) {
      WeightUnitRequest.KG -> Weight.WeightUnit.KILOGRAM
    }
  )
}

enum class WeightUnitRequest {
  KG,
}

fun List<WeightTicketLineRequest>.toDomain(catalogItemRepository: CatalogItemJpaRepository): WeightTicketLines {
  return WeightTicketLines(
    this.map { line ->
      val catalogItem = catalogItemRepository.findById(line.catalogItemId)
        .orElseThrow { IllegalArgumentException("Catalogusitem niet gevonden: ${line.catalogItemId}") }
      WeightTicketLine(
        waste = line.wasteStreamNumber?.let { WasteStreamNumber(it) },
        weight = line.weight.toDomain(),
        catalogItemId = line.catalogItemId,
        catalogItemType = catalogItem.type,
      )
    }
  )
}

data class WeightTicketProductLineRequest(
  val catalogItemId: UUID,
  val quantity: String,
  val unit: String,
)

fun List<WeightTicketProductLineRequest>.toDomain(catalogItemRepository: CatalogItemJpaRepository): WeightTicketProductLines {
  return WeightTicketProductLines(
    this.map { line ->
      val catalogItem = catalogItemRepository.findById(line.catalogItemId)
        .orElseThrow { IllegalArgumentException("Catalogusitem niet gevonden: ${line.catalogItemId}") }
      WeightTicketProductLine(
        catalogItemId = line.catalogItemId,
        catalogItemType = catalogItem.type,
        quantity = BigDecimal(line.quantity),
        unit = line.unit,
      )
    }
  )
}
