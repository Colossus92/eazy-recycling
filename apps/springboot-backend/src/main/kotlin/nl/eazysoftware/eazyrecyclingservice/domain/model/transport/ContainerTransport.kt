package nl.eazysoftware.eazyrecyclingservice.domain.model.transport

import kotlinx.datetime.Instant
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainer
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto.Status
import java.util.*
import kotlin.time.Duration

class ContainerTransport(
  val transportId: TransportId,

  /**
   * Used for human-readable display in the UI
   */
  val displayNumber: TransportDisplayNumber,

  /**
   * The party client ordering the transport.
   */
  val consignorParty: CompanyId,

  /**
   * The party executing the transport.
   */
  val carrierParty: CompanyId,

  val pickupLocation: Location, //TODO: implement the branch(project location) possibilty

  val pickupDateTime: Instant,

  val deliveryLocation: Location,

  val deliveryDateTime: Instant,

  val transportType: TransportType,

  val wasteContainer: WasteContainer,

  val truck: Truck?,

  val driver: UserId?,

  val note: Note,

  val transportHours: Duration?,

  val updatedAt: Instant,

  /**
   * Used for ordering transports within the planning
   */
  val sequenceNumber: Int,

  ) {

  fun getStatus(): Status {
    if (driver == null || truck == null) {
      return Status.UNPLANNED
    }

    if (transportHours != null) {
      return Status.FINISHED
    }

    return Status.PLANNED

  }
}

data class TransportDisplayNumber(
  val value: String,
)

data class TransportId(
  val uuid: UUID,
)

enum class TransportType {
  CONTAINER,
  WASTE,
}


enum class ContainerOperation {
  EXCHANGE,
  PICKUP,
  EMPTY,
  DELIVERY,
}

