package nl.eazysoftware.eazyrecyclingservice.controller.transport.containertransport

import jakarta.validation.Valid
import kotlinx.datetime.toKotlinInstant
import nl.eazysoftware.eazyrecyclingservice.application.usecase.transport.CreateContainerTransport
import nl.eazysoftware.eazyrecyclingservice.application.usecase.transport.CreateContainerTransportCommand
import nl.eazysoftware.eazyrecyclingservice.application.usecase.transport.UpdateContainerTransport
import nl.eazysoftware.eazyrecyclingservice.application.usecase.transport.UpdateContainerTransportCommand
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.controller.transport.containertransport.ContainerTransportRequest
import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportId
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ContainerTransports
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.ZoneId
import java.util.*

@RestController
@RequestMapping("/transport")
class ContainerTransportController(
  private val createContainerTransport: CreateContainerTransport,
  private val updateContainerTransport: UpdateContainerTransport,
  private val containerTransports: ContainerTransports
) {

  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @PostMapping("/container")
  @ResponseStatus(HttpStatus.CREATED)
  fun createContainerTransport(@Valid @RequestBody request: ContainerTransportRequest): CreateContainerTransportResponse {
    val result = createContainerTransport.handle(request.toCommand())
    return CreateContainerTransportResponse(transportId = result.transportId.uuid)
  }

  @PreAuthorize(HAS_ANY_ROLE)
  @PutMapping("/container/{id}")
  fun updateContainerTransport(
    @PathVariable id: UUID,
    @Valid @RequestBody request: ContainerTransportRequest
  ): UpdateContainerTransportResponse {
    // Check authorization before updating
    checkAuthorization(id)

    val result = updateContainerTransport.handle(request.toUpdateCommand(id))
    return UpdateContainerTransportResponse(
      transportId = result.transportId.uuid,
      status = result.status
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
          val transport = containerTransports.findById(TransportId(transportId))
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

data class CreateContainerTransportResponse(
  val transportId: UUID
)

data class UpdateContainerTransportResponse(
  val transportId: UUID,
  val status: String
)

/**
 * Extension function to map request to create command
 */
fun ContainerTransportRequest.toCommand(): CreateContainerTransportCommand {
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

/**
 * Extension function to map request to update command
 */
fun ContainerTransportRequest.toUpdateCommand(transportId: UUID): UpdateContainerTransportCommand {
  return UpdateContainerTransportCommand(
    transportId = TransportId(transportId),
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
    deliveryProjectLocationId = this.deliveryProjectLocationId,
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
