package nl.eazysoftware.eazyrecyclingservice.controller.transport.containertransport

import jakarta.validation.Valid
import kotlinx.datetime.toKotlinInstant
import nl.eazysoftware.eazyrecyclingservice.application.usecase.transport.CreateContainerTransport
import nl.eazysoftware.eazyrecyclingservice.application.usecase.transport.CreateContainerTransportCommand
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.controller.transport.CreateContainerTransportRequest
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.ZoneId
import java.util.*

@RestController
@RequestMapping("/transport")
class ContainerTransportController(
  private val createContainerTransport: CreateContainerTransport
) {

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @PostMapping("/container")
  @ResponseStatus(HttpStatus.CREATED)
  fun createContainerTransport(@Valid @RequestBody request: CreateContainerTransportRequest): CreateContainerTransportResponse {
    val result = createContainerTransport.handle(request.toCommand())
    return CreateContainerTransportResponse(transportId = result.transportId.uuid)
  }

}

data class CreateContainerTransportResponse(
  val transportId: UUID
)

/**
 * Extension function to map request to command
 */
fun CreateContainerTransportRequest.toCommand(): CreateContainerTransportCommand {
  return CreateContainerTransportCommand(
    consignorParty = CompanyId(this.consignorPartyId),
    carrierParty = CompanyId(this.carrierPartyId),
    pickupCompanyId = this.pickupCompanyId?.let { CompanyId(it) },
    pickupProjectLocationId = this.pickupProjectLocationId,
    pickupStreetName = this.pickupStreet,
    pickupBuildingNumber = this.pickupBuildingNumber,
    pickupBuildingNumberAddition = this.pickupBuildingNumberAddition,
    pickupPostalCode = this.pickupPostalCode,
    pickupCity = this.pickupCity,
    pickupDescription = this.pickupDescription,
    pickupDateTime = this.pickupDateTime.atZone(ZoneId.systemDefault()).toInstant().toKotlinInstant(),
    deliveryCompanyId = this.deliveryCompanyId?.let { CompanyId(it) },
    deliveryProjectLocationId = deliveryProjectLocationId,
    deliveryStreetName = this.deliveryStreet,
    deliveryBuildingNumber = this.deliveryBuildingNumber,
    deliveryBuildingNumberAddition = this.deliveryBuildingNumberAddition,
    deliveryPostalCode = this.deliveryPostalCode,
    deliveryDescription = this.deliveryDescription,
    deliveryCity = this.deliveryCity,
    deliveryDateTime = this.deliveryDateTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toKotlinInstant()
      ?: kotlinx.datetime.Clock.System.now(),
    transportType = this.transportType,
    wasteContainer = this.containerId?.let { WasteContainerId(it) },
    truck = this.truckId?.let { LicensePlate(it) },
    driver = this.driverId?.let { UserId(it) },
    note = Note(this.note),
  )
}
