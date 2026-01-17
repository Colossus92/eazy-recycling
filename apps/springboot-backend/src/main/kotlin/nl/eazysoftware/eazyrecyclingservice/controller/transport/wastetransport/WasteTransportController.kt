package nl.eazysoftware.eazyrecyclingservice.controller.transport.wastetransport

import jakarta.validation.Valid
import nl.eazysoftware.eazyrecyclingservice.application.usecase.transport.*
import nl.eazysoftware.eazyrecyclingservice.config.clock.toCetKotlinInstant
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.GoodsItem
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportId
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteTransports
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.*
import kotlin.time.Clock

@RestController
@RequestMapping("/transport")
class WasteTransportController(
  private val createWasteTransport: CreateWasteTransport,
  private val createWasteTransportFromWeightTicket: CreateWasteTransportFromWeightTicket,
  private val getWasteTransportsFromWeightTicket: nl.eazysoftware.eazyrecyclingservice.application.query.GetWasteTransportsFromWeightTicket,
  private val updateWasteTransport: UpdateWasteTransport,
  private val wasteTransports: WasteTransports
) {

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @PostMapping("/waste")
  @ResponseStatus(HttpStatus.CREATED)
  fun createWasteTransport(@Valid @RequestBody request: WasteTransportRequest): CreateWasteTransportResponse {
    val result = createWasteTransport.handle(request.toCreateCommand())
    return CreateWasteTransportResponse(transportId = result.transportId.uuid)
  }

  @PreAuthorize(HAS_ANY_ROLE)
  @PutMapping("/waste/{id}")
  fun updateWasteTransport(
    @PathVariable id: UUID,
    @Valid @RequestBody request: WasteTransportRequest
  ): UpdateWasteTransportResponse {
    // Check authorization before updating
    checkAuthorization(id)

    val result = updateWasteTransport.handle(request.toUpdateCommand(id))
    return UpdateWasteTransportResponse(
      transportId = result.transportId.uuid,
      status = result.status
    )
  }

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @PostMapping("/waste/from-weight-ticket")
  @ResponseStatus(HttpStatus.CREATED)
  fun createWasteTransportFromWeightTicket(
    @Valid @RequestBody request: CreateWasteTransportFromWeightTicketRequest
  ): CreateFromWeightTicketResponse {
    val command = CreateWasteTransportFromWeightTicketCommand(
      weightTicketNumber = request.weightTicketId,
      pickupDateTime = request.pickupDateTime,
      deliveryDateTime = request.deliveryDateTime
    )
    val result = createWasteTransportFromWeightTicket.execute(command)
    return CreateFromWeightTicketResponse(
      transportId = result.transportId.uuid,
      displayNumber = result.displayNumber,
      pickupDateTime = result.pickupDateTime,
    )
  }

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @GetMapping("/waste/from-weight-ticket")
  fun getWasteTransportsByWeightTicketId(
    @RequestParam weightTicketId: Long
  ): WeightTicketTransportsResponse {
    val transports = getWasteTransportsFromWeightTicket.execute(weightTicketId)
    return WeightTicketTransportsResponse(
      transports = transports.map { view ->
        WeightTicketTransportsResponse.WeightTicketTransport(
          transportId = view.transportId,
          displayNumber = view.displayNumber,
          pickupDateTime = view.pickupDateTime,
          status = view.status,
          weightTicketNumber = view.weightTicketNumber
        )
      }
    )
  }

  data class WeightTicketTransportsResponse(
    val transports: List<WeightTicketTransport>
  ) {

    data class WeightTicketTransport(
      val transportId: UUID,
      val displayNumber: String,
      val pickupDateTime: LocalDateTime,
      val status: String,
      val weightTicketNumber: Long
    )
  }

  private fun checkAuthorization(transportId: UUID) {
    when (val authentication = SecurityContextHolder.getContext().authentication) {
      is JwtAuthenticationToken -> {
        val userIdFromToken = authentication.token.subject
        val isAdminOrPlanner = authentication.authorities.any {
          it.authority == Roles.ADMIN || it.authority == Roles.PLANNER
        }

        // If user is not admin/planner, check if they are the driver
        if (!isAdminOrPlanner) {
          val transport = wasteTransports.findById(TransportId(transportId))
            ?: throw ResponseStatusException(
              HttpStatus.NOT_FOUND,
              "Transport met id $transportId niet gevonden"
            )

          if (transport.driver?.uuid?.toString() != userIdFromToken) {
            throw ResponseStatusException(
              HttpStatus.FORBIDDEN,
              "Geen toegang: je hebt geen toegang tot dit transport"
            )
          }
        }
      }

      else -> throw ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "Geen toegang: Je moet ingelogd zijn om dit transport te zien"
      )
    }
  }
}

data class CreateWasteTransportResponse(
  val transportId: UUID
)

data class CreateFromWeightTicketResponse(
  val transportId: UUID,
  val displayNumber: String,
  val pickupDateTime: LocalDateTime,
)

data class UpdateWasteTransportResponse(
  val transportId: UUID,
  val status: String
)

data class CreateWasteTransportFromWeightTicketRequest(
  val weightTicketId: Long,
  val pickupDateTime: LocalDateTime,
  val deliveryDateTime: LocalDateTime?,
)

/**
 * Extension function to map request to create command
 */
fun WasteTransportRequest.toCreateCommand(): CreateWasteTransportCommand {
  require(this.goods.isNotEmpty()) {
    "Afvalstroom is verplicht voor een afvaltransport, kies een afvalstroomnummer"
  }

  return CreateWasteTransportCommand(
    carrierParty = CompanyId(this.carrierPartyId),
    pickupDateTime = this.pickupDateTime.toCetKotlinInstant(),
    deliveryDateTime = this.deliveryDateTime?.toCetKotlinInstant()
      ?: Clock.System.now(),
    transportType = this.transportType,
    goods = this.goods.map {
      GoodsItem(
        wasteStreamNumber = WasteStreamNumber(it.wasteStreamNumber),
        netNetWeight = it.weight,
        unit = it.unit,
        quantity = it.quantity
      )
    },
    wasteContainer = this.containerId?.let { WasteContainerId(it) },
    containerOperation = this.containerOperation,
    truck = this.truckId?.let { LicensePlate(it) },
    driver = this.driverId?.let { UserId(it) },
    note = Note(this.note),
  )
}

/**
 * Extension function to map request to update command
 */
fun WasteTransportRequest.toUpdateCommand(transportId: UUID): UpdateWasteTransportCommand {
  require(this.goods.isNotEmpty()) {
    "Afvalstroomnummer is verplicht voor een afvaltransport"
  }

  return UpdateWasteTransportCommand(
    transportId = TransportId(transportId),
    carrierParty = CompanyId(this.carrierPartyId),
    pickupDateTime = this.pickupDateTime.toCetKotlinInstant(),
    deliveryDateTime = this.deliveryDateTime?.toCetKotlinInstant()
      ?: Clock.System.now(),
    transportType = this.transportType,
    goods = this.goods.map {
      GoodsItem(
        wasteStreamNumber = WasteStreamNumber(it.wasteStreamNumber),
        netNetWeight = it.weight,
        unit = it.unit,
        quantity = it.quantity
      )
    },
    wasteContainer = this.containerId?.let { WasteContainerId(it) },
    containerOperation = this.containerOperation,
    truck = this.truckId?.let { LicensePlate(it) },
    driver = this.driverId?.let { UserId(it) },
    note = Note(this.note),
  )
}
