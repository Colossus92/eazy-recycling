package nl.eazysoftware.eazyrecyclingservice.application.usecase.transport

import jakarta.persistence.EntityNotFoundException
import kotlinx.datetime.Instant
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
import nl.eazysoftware.eazyrecyclingservice.domain.service.PdfGenerationClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
  val pickupLocation: PickupLocationCommand,
  val pickupDateTime: Instant,
  val deliveryLocation: PickupLocationCommand,
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
  private val projectLocations: ProjectLocations,
  private val pdfGenerationClient: PdfGenerationClient,
  private val companies: Companies,
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
      transportHours = existingTransport.transportHours,
      updatedAt = kotlinx.datetime.Clock.System.now(),
      sequenceNumber = existingTransport.sequenceNumber
    )

    val savedTransport = containerTransports.save(updatedTransport)
    pdfGenerationClient.triggerPdfGeneration(savedTransport.transportId.uuid, "empty")

    return UpdateContainerTransportResult(
      transportId = savedTransport.transportId,
      status = savedTransport.getStatus().name
    )
  }
}
