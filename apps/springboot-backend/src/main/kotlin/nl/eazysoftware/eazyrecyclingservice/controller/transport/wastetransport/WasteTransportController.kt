package nl.eazysoftware.eazyrecyclingservice.controller.transport.wastetransport

import jakarta.validation.Valid
import kotlinx.datetime.toKotlinInstant
import nl.eazysoftware.eazyrecyclingservice.application.usecase.transport.CreateWasteTransport
import nl.eazysoftware.eazyrecyclingservice.application.usecase.transport.CreateWasteTransportCommand
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.GoodsItem
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.ZoneId
import java.util.*

@RestController
@RequestMapping("/transport")
class WasteTransportController(
  private val createWasteTransport: CreateWasteTransport
) {

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @PostMapping("/waste")
  @ResponseStatus(HttpStatus.CREATED)
  fun createWasteTransport(@Valid @RequestBody request: WasteTransportRequest): CreateWasteTransportResponse {
    val result = createWasteTransport.handle(request.toCommand())
    return CreateWasteTransportResponse(transportId = result.transportId.uuid)
  }
}

data class CreateWasteTransportResponse(
  val transportId: UUID
)

/**
 * Extension function to map request to create command
 */
fun WasteTransportRequest.toCommand(): CreateWasteTransportCommand {
  require(this.wasteStreamNumber != null) {
    "Afvalstroomnummer is verplicht voor een afvaltransport"
  }

  return CreateWasteTransportCommand(
    carrierParty = CompanyId(this.carrierPartyId),
    pickupDateTime = this.pickupDateTime.atZone(ZoneId.systemDefault()).toInstant().toKotlinInstant(),
    deliveryDateTime = this.deliveryDateTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toKotlinInstant()
      ?: kotlinx.datetime.Clock.System.now(),
    transportType = this.transportType,
    goodsItem = GoodsItem(
      wasteStreamNumber = WasteStreamNumber(this.wasteStreamNumber),
      netNetWeight = this.weight,
      unit = this.unit,
      quantity = this.quantity
    ),
    wasteContainer = this.containerId?.let { WasteContainerId(it) },
    containerOperation = this.containerOperation,
    truck = this.truckId?.let { LicensePlate(it) },
    driver = this.driverId?.let { UserId(it) },
    note = Note(this.note),
  )
}
