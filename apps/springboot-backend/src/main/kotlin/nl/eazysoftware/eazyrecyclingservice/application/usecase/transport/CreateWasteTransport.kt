package nl.eazysoftware.eazyrecyclingservice.application.usecase.transport

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteTransports
import nl.eazysoftware.eazyrecyclingservice.domain.service.PdfGenerationClient
import nl.eazysoftware.eazyrecyclingservice.domain.service.TransportDisplayNumberGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Instant

interface CreateWasteTransport {
  fun handle(cmd: CreateWasteTransportCommand): CreateWasteTransportResult
}

/**
 * Command for creating a waste transport.
 * This follows the hexagonal architecture pattern where the command contains domain objects.
 */
data class CreateWasteTransportCommand(
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

data class CreateWasteTransportResult(
  val transportId: TransportId
)

@Service
class CreateWasteTransportService(
  private val wasteTransports: WasteTransports,
  private val wasteTransportFactory: WasteTransportFactory,
  private val wasteStreams: WasteStreams,
  private val transportDisplayNumberGenerator: TransportDisplayNumberGenerator,
  private val pdfGenerationClient: PdfGenerationClient,
) : CreateWasteTransport {

  @Transactional
  override fun handle(cmd: CreateWasteTransportCommand): CreateWasteTransportResult {
    val displayNumber = transportDisplayNumberGenerator.generateDisplayNumber()
    val wasteStreams = cmd.goods.map {
      wasteStreams.findByNumber(it.wasteStreamNumber)
        ?: throw EntityNotFoundException("Afvalstroom met nummer ${it.wasteStreamNumber} niet gevonden")
    }

    val wasteTransport = wasteTransportFactory.create(
      displayNumber = displayNumber,
      carrierParty = cmd.carrierParty,
      pickupDateTime = cmd.pickupDateTime,
      deliveryDateTime = cmd.deliveryDateTime,
      goods = cmd.goods,
      wasteStreams = wasteStreams,
      wasteContainer = cmd.wasteContainer,
      containerOperation = cmd.containerOperation,
      truck = cmd.truck,
      driver = cmd.driver,
      note = cmd.note,
      transportHours = null,
      sequenceNumber = 9999 // Create as last in line
    )

    val savedTransport = wasteTransports.save(wasteTransport)
    pdfGenerationClient.triggerPdfGeneration(savedTransport.transportId.uuid, "empty")

    return CreateWasteTransportResult(
      transportId = savedTransport.transportId
    )
  }
}
