package nl.eazysoftware.eazyrecyclingservice.application.usecase.transport

import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream.PickupLocationCommand
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream.toDomain
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ContainerTransports
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.domain.service.TransportDisplayNumberGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Clock
import kotlin.time.Instant

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
  val pickupLocation: PickupLocationCommand,
  val pickupDateTime: Instant,
  val deliveryLocation: PickupLocationCommand,
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
  private val transportDisplayNumberGenerator: TransportDisplayNumberGenerator,
  private val companies: Companies,
  private val projectLocations: ProjectLocations,
) : CreateContainerTransport {

  @Transactional
  override fun handle(cmd: CreateContainerTransportCommand): CreateContainerTransportResult {
    val displayNumber = transportDisplayNumberGenerator.generateDisplayNumber()

    val containerTransport = ContainerTransport.create(
      displayNumber = displayNumber,
      consignorParty = cmd.consignorParty,
      carrierParty = cmd.carrierParty,
      pickupLocation = cmd.pickupLocation.toDomain(companies, projectLocations),
      pickupDateTime = cmd.pickupDateTime,
      deliveryLocation = cmd.deliveryLocation.toDomain(companies, projectLocations),
      deliveryDateTime = cmd.deliveryDateTime,
      transportType = cmd.transportType,
      wasteContainer = cmd.wasteContainer,
      containerOperation = cmd.containerOperation,
      truck = cmd.truck,
      driver = cmd.driver,
      note = cmd.note,
      transportHours = null,
      updatedAt = Clock.System.now(),
      sequenceNumber = 9999 // Create as last in line
    )

    val savedTransport = containerTransports.save(containerTransport)

    return CreateContainerTransportResult(
      transportId = savedTransport.transportId
    )
  }
}
