package nl.eazysoftware.eazyrecyclingservice.application.usecase.transport

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteTransports
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Clock
import kotlin.time.Instant

interface UpdateWasteTransport {
  fun handle(cmd: UpdateWasteTransportCommand): UpdateWasteTransportResult
}

/**
 * Command for updating a waste transport.
 * This follows the hexagonal architecture pattern where the command contains domain objects.
 */
data class UpdateWasteTransportCommand(
  val transportId: TransportId,
  val carrierParty: CompanyId,
  val pickupDateTime: Instant,
  val deliveryDateTime: Instant,
  val transportType: TransportType,
  val goods: List<GoodsItem>,
  val wasteContainer: WasteContainerId?,
  val containerOperation: ContainerOperation?,
  val truck: LicensePlate?,
  val driver: UserId?,
  val note: Note,
)

data class UpdateWasteTransportResult(
  val transportId: TransportId,
  val status: String
)

@Service
class UpdateWasteTransportService(
  private val wasteTransports: WasteTransports,
) : UpdateWasteTransport {

  @Transactional
  override fun handle(cmd: UpdateWasteTransportCommand): UpdateWasteTransportResult {
    // Load existing transport
    val existingTransport = wasteTransports.findById(cmd.transportId)
      ?: throw EntityNotFoundException("Transport met id ${cmd.transportId.uuid} niet gevonden")

    // Check if transport is already finished
    if (existingTransport.getStatus() == TransportStatus.FINISHED) {
      throw IllegalStateException("Transport is gereed gemeld en kan niet meer worden aangepast.")
    }

    // Create updated transport with new values
    val updatedTransport = WasteTransport(
      transportId = existingTransport.transportId,
      displayNumber = existingTransport.displayNumber,
      carrierParty = cmd.carrierParty,
      pickupDateTime = cmd.pickupDateTime,
      deliveryDateTime = cmd.deliveryDateTime,
      transportType = cmd.transportType,
      goods = cmd.goods,
      wasteContainer = cmd.wasteContainer,
      containerOperation = cmd.containerOperation,
      truck = cmd.truck,
      driver = cmd.driver,
      note = cmd.note,
      transportHours = existingTransport.transportHours,
      updatedAt = Clock.System.now(),
      sequenceNumber = existingTransport.sequenceNumber
    )

    val savedTransport = wasteTransports.save(updatedTransport)

    return UpdateWasteTransportResult(
      transportId = savedTransport.transportId,
      status = savedTransport.getStatus().name
    )
  }
}
