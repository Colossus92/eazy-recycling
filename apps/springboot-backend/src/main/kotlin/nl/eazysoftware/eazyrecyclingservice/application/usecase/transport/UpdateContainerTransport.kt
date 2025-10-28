package nl.eazysoftware.eazyrecyclingservice.application.usecase.transport

import jakarta.persistence.EntityNotFoundException
import kotlinx.datetime.Instant
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.LocationFactory
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ContainerTransports
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface UpdateContainerTransport {
  fun handle(cmd: UpdateContainerTransportCommand): UpdateContainerTransportResult
}

/**
 * Command for updating a container transport.
 * This follows the hexagonal architecture pattern where the command contains domain objects.
 */
data class UpdateContainerTransportCommand(
  val transportId: TransportId,
  val consignorParty: CompanyId,
  val carrierParty: CompanyId,
  val pickupCompanyId: CompanyId? = null,
  val pickupProjectLocationId: UUID? = null,
  val pickupStreetName: String?,
  val pickupBuildingNumber: String?,
  val pickupBuildingNumberAddition: String? = null,
  val pickupPostalCode: String?,
  val pickupCity: String?,
  val pickupDescription: String? = null,
  val pickupDateTime: Instant,
  val deliveryCompanyId: CompanyId?,
  val deliveryProjectLocationId: UUID? = null,
  val deliveryStreetName: String,
  val deliveryBuildingNumber: String,
  val deliveryBuildingNumberAddition: String? = null,
  val deliveryPostalCode: String,
  val deliveryCity: String,
  val deliveryDescription: String? = null,
  val deliveryDateTime: Instant?,
  val transportType: TransportType,
  val wasteContainer: WasteContainerId?,
  val containerOperation: ContainerOperation?,
  val truck: LicensePlate?,
  val driver: UserId?,
  val note: Note,
)

data class UpdateContainerTransportResult(
  val transportId: TransportId,
  val status: String
)

@Service
class UpdateContainerTransportService(
  private val containerTransports: ContainerTransports,
  private val locations: ProjectLocations
) : UpdateContainerTransport {

  @Transactional
  override fun handle(cmd: UpdateContainerTransportCommand): UpdateContainerTransportResult {
    // Load existing transport
    val existingTransport = containerTransports.findById(cmd.transportId)
      ?: throw EntityNotFoundException("Transport met id ${cmd.transportId.uuid} niet gevonden")

    // Check if transport is already finished
    if (existingTransport.getStatus() == TransportStatus.FINISHED) {
      throw IllegalStateException("Transport is gereed gemeld en kan niet meer worden aangepast.")
    }

    // Create updated transport with new values
    val updatedTransport = ContainerTransport(
      transportId = existingTransport.transportId,
      displayNumber = existingTransport.displayNumber,
      consignorParty = cmd.consignorParty,
      carrierParty = cmd.carrierParty,
      pickupLocation = createPickupLocation(cmd),
      pickupDateTime = cmd.pickupDateTime,
      deliveryLocation = createDeliveryLocation(cmd),
      deliveryDateTime = cmd.deliveryDateTime,
      transportType = cmd.transportType,
      wasteContainer = cmd.wasteContainer,
      containerOperation = cmd.containerOperation,
      truck = cmd.truck,
      driver = cmd.driver,
      note = cmd.note,
      transportHours = existingTransport.transportHours,
      updatedAt = kotlinx.datetime.Clock.System.now(),
      sequenceNumber = existingTransport.sequenceNumber
    )

    val savedTransport = containerTransports.save(updatedTransport)

    return UpdateContainerTransportResult(
      transportId = savedTransport.transportId,
      status = savedTransport.getStatus().name
    )
  }

  private fun createDeliveryLocation(cmd: UpdateContainerTransportCommand): Location = createLocation(
    cmd.deliveryProjectLocationId,
    cmd.deliveryCompanyId,
    cmd.deliveryStreetName,
    cmd.deliveryBuildingNumber,
    cmd.deliveryBuildingNumberAddition,
    cmd.deliveryPostalCode,
    cmd.deliveryCity,
    cmd.deliveryDescription
  )

  private fun createPickupLocation(cmd: UpdateContainerTransportCommand): Location = createLocation(
    cmd.pickupProjectLocationId,
    cmd.pickupCompanyId,
    cmd.pickupStreetName,
    cmd.pickupBuildingNumber,
    cmd.pickupBuildingNumberAddition,
    cmd.pickupPostalCode,
    cmd.pickupCity,
    cmd.pickupDescription
  )

  private fun createLocation(
    projectLocationId: UUID? = null,
    companyId: CompanyId?,
    streetName: String?,
    buildingNumber: String?,
    buildingNumberAddition: String? = null,
    postalCode: String?,
    city: String?,
    description: String? = null,
  ): Location {
    if (projectLocationId != null) {
      val existingLocation = locations.findById(projectLocationId)
        ?: throw EntityNotFoundException("Geen projectlocatie gevonden voor id ${projectLocationId}")

      require(existingLocation.companyId == companyId) {
        "Projectlocatie met id ${projectLocationId} is niet van bedrijf met id ${companyId?.uuid}"
      }

      return existingLocation
    }

    return LocationFactory.create(
      companyId = companyId,
      streetName = streetName,
      buildingNumber = buildingNumber,
      buildingNumberAddition = buildingNumberAddition,
      postalCode = postalCode,
      description = description,
      city = city
    )
  }
}
