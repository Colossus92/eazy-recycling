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
import nl.eazysoftware.eazyrecyclingservice.domain.service.TransportDisplayNumberGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface CreateContainerTransport {
  fun handle(cmd: CreateContainerTransportCommand): CreateContainerTransportResult
}

/**
 * Command for creating a container transport.
 * This follows the hexagonal architecture pattern where the command contains domain objects.
 */
data class CreateContainerTransportCommand(
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
  val containerOperation: ContainerOperation,
  val truck: LicensePlate?,
  val driver: UserId?,
  val note: Note,
)

data class CreateContainerTransportResult(
  val transportId: TransportId
)

@Service
class CreateContainerTransportService(
  private val containerTransports: ContainerTransports,
  private val locations: ProjectLocations,
  private val locationFactory: LocationFactory,
  private val transportDisplayNumberGenerator: TransportDisplayNumberGenerator,
) : CreateContainerTransport {

  @Transactional
  override fun handle(cmd: CreateContainerTransportCommand): CreateContainerTransportResult {
    val displayNumber = transportDisplayNumberGenerator.generateDisplayNumber()

    val containerTransport = ContainerTransport.create(
      displayNumber = displayNumber,
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
      transportHours = null,
      updatedAt = kotlinx.datetime.Clock.System.now(),
      sequenceNumber = 9999 // Create as last in line
    )

    val savedTransport = containerTransports.save(containerTransport)

    return CreateContainerTransportResult(
      transportId = savedTransport.transportId
    )
  }

  private fun createDeliveryLocation(cmd: CreateContainerTransportCommand): Location = createLocation(
    cmd.deliveryProjectLocationId,
    cmd.deliveryCompanyId,
    cmd.deliveryStreetName,
    cmd.deliveryBuildingNumber,
    cmd.deliveryBuildingNumberAddition,
    cmd.deliveryPostalCode,
    cmd.deliveryCity,
    cmd.deliveryDescription
  )

  private fun createPickupLocation(cmd: CreateContainerTransportCommand): Location = createLocation(
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

      return existingLocation.toSnapshot()
    }

    return locationFactory.create(
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
