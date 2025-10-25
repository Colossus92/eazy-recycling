package nl.eazysoftware.eazyrecyclingservice.controller.transport.containertransport

import jakarta.validation.Valid
import kotlinx.datetime.toKotlinInstant
import nl.eazysoftware.eazyrecyclingservice.application.usecase.transport.CreateContainerTransport
import nl.eazysoftware.eazyrecyclingservice.application.usecase.transport.CreateContainerTransportCommand
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.controller.transport.CreateContainerTransportRequest
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.LocationFactory
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
    pickupLocation = LocationFactory.create(
      companyId = this.pickupCompanyId?.let { CompanyId(it) },
      streetName = this.pickupStreet,
      buildingNumber = this.pickupBuildingNumber,
      buildingNumberAddition = this.pickupBuildingNumberAddition,
      postalCode = this.pickupPostalCode,
      description = this.pickupCity,
      city = this.pickupDescription,
    ),
    pickupDateTime = this.pickupDateTime.atZone(ZoneId.systemDefault()).toInstant().toKotlinInstant(),
    deliveryLocation = LocationFactory.create(
      companyId = this.deliveryCompanyId?.let { CompanyId(it) },
      streetName = this.deliveryStreet,
      buildingNumber = this.deliveryBuildingNumber,
      buildingNumberAddition = this.deliveryBuildingNumberAddition,
      postalCode = this.deliveryPostalCode,
      description = this.deliveryDescription,
      city = this.deliveryCity,
    ),
    deliveryDateTime = this.deliveryDateTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toKotlinInstant()
      ?: kotlinx.datetime.Clock.System.now(),
    transportType = this.transportType,
    wasteContainer = this.containerId?.let { WasteContainerId(it) },
    truck = this.truckId?.let { LicensePlate(it) },
    driver = this.driverId?.let { UserId(it) },
    note = Note(this.note),
  )
}
