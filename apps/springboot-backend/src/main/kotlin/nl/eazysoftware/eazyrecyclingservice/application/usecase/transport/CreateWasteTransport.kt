package nl.eazysoftware.eazyrecyclingservice.application.usecase.transport

import kotlinx.datetime.Instant
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteTransports
import nl.eazysoftware.eazyrecyclingservice.domain.service.PdfGenerationClient
import nl.eazysoftware.eazyrecyclingservice.domain.service.TransportDisplayNumberGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
  val goodsItem: GoodsItem,
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
  private val transportDisplayNumberGenerator: TransportDisplayNumberGenerator,
  private val pdfGenerationClient: PdfGenerationClient,
) : CreateWasteTransport {

  @Transactional
  override fun handle(cmd: CreateWasteTransportCommand): CreateWasteTransportResult {
    val displayNumber = transportDisplayNumberGenerator.generateDisplayNumber()

    val wasteTransport = WasteTransport.create(
      displayNumber = displayNumber,
      carrierParty = cmd.carrierParty,
      pickupDateTime = cmd.pickupDateTime,
      deliveryDateTime = cmd.deliveryDateTime,
      transportType = cmd.transportType,
      goodsItem = cmd.goodsItem,
      wasteContainer = cmd.wasteContainer,
      containerOperation = cmd.containerOperation,
      truck = cmd.truck,
      driver = cmd.driver,
      note = cmd.note,
      transportHours = null,
      updatedAt = kotlinx.datetime.Clock.System.now(),
      sequenceNumber = 9999 // Create as last in line
    )

    val savedTransport = wasteTransports.save(wasteTransport)
    pdfGenerationClient.triggerPdfGeneration(savedTransport.transportId.uuid, "empty")

    return CreateWasteTransportResult(
      transportId = savedTransport.transportId
    )
  }
}
