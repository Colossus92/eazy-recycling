package nl.eazysoftware.eazyrecyclingservice.application.usecase.transport

import kotlinx.datetime.Instant
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ContainerTransports
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

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
  val pickupLocation: Location,
  val pickupDateTime: Instant,
  val deliveryLocation: Location,
  val deliveryDateTime: Instant,
  val transportType: TransportType,
  val wasteContainer: WasteContainerId?,
  val truck: LicensePlate?,
  val driver: UserId?,
  val note: Note,
)

data class CreateContainerTransportResult(
  val transportId: TransportId
)

@Service
class CreateContainerTransportService(
  private val containerTransports: ContainerTransports
) : CreateContainerTransport {

  @Transactional
  override fun handle(cmd: CreateContainerTransportCommand): CreateContainerTransportResult {
    val containerTransport = ContainerTransport(
      consignorParty = cmd.consignorParty,
      carrierParty = cmd.carrierParty,
      pickupLocation = cmd.pickupLocation,
      pickupDateTime = cmd.pickupDateTime,
      deliveryLocation = cmd.deliveryLocation,
      deliveryDateTime = cmd.deliveryDateTime,
      transportType = cmd.transportType,
      wasteContainer = cmd.wasteContainer,
      truck = cmd.truck,
      driver = cmd.driver,
      note = cmd.note,
      transportHours = null,
      updatedAt = kotlinx.datetime.Clock.System.now(),
      sequenceNumber = 9999 // Create as last in line
    )

    val savedTransport = containerTransports.save(containerTransport)

    return CreateContainerTransportResult(
      transportId = savedTransport.transportId ?: throw IllegalStateException("Transport ID should be set after save")
    )
  }
}
