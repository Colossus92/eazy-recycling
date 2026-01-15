package nl.eazysoftware.eazyrecyclingservice.application.usecase.transport

import jakarta.persistence.EntityNotFoundException
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteTransports
import nl.eazysoftware.eazyrecyclingservice.application.jobs.EdgeFunctionJobService
import org.jobrunr.scheduling.BackgroundJob
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant

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
  val deliveryDateTime: Instant?,
  val transportType: TransportType,
  val goods: List<GoodsItem>,
  val wasteContainer: WasteContainerId?,
  val containerOperation: ContainerOperation?,
  val truck: LicensePlate?,
  val driver: UserId?,
  val note: Note? = null,
  val weightTicketId: WeightTicketId? = null,
)

data class CreateWasteTransportResult(
  val transportId: TransportId,
  val pickupDateTime: LocalDateTime,
  val displayNumber: String,
) {
}

@Service
class CreateWasteTransportService(
  private val wasteTransports: WasteTransports,
  private val wasteTransportFactory: WasteTransportFactory,
  private val wasteStreams: WasteStreams,
) : CreateWasteTransport {

  @Transactional
  override fun handle(cmd: CreateWasteTransportCommand): CreateWasteTransportResult {
    val wasteStreams = cmd.goods.map {
      wasteStreams.findByNumber(it.wasteStreamNumber)
        ?: throw EntityNotFoundException("Afvalstroom met nummer ${it.wasteStreamNumber} niet gevonden")
    }

    val wasteTransport = wasteTransportFactory.create(
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
      driverNote = null,
      weightTicketId = cmd.weightTicketId,
      sequenceNumber = 9999 // Create as last in line
    )

    val savedTransport = wasteTransports.save(wasteTransport)
    
    BackgroundJob.enqueue<EdgeFunctionJobService> {
      it.executeGenerateWaybillPdfJob(savedTransport.transportId.uuid.toString(), "empty")
    }

    return CreateWasteTransportResult(
      transportId = savedTransport.transportId,
      displayNumber = savedTransport.displayNumber?.value ?: "Onbekend",
      pickupDateTime = savedTransport.pickupDateTime.toLocalDateTime(TimeZone.of("Europe/Amsterdam")).toJavaLocalDateTime()
    )
  }
}
