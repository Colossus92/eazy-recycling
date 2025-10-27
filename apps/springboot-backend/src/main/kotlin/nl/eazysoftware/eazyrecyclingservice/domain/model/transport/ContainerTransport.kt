package nl.eazysoftware.eazyrecyclingservice.domain.model.transport

import kotlinx.datetime.Instant
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import kotlin.time.Duration

class ContainerTransport(
  val transportId: TransportId? = null, // TODO remove UUID generation in DTO

  /**
   * Used for human-readable display in the UI
   */
  val displayNumber: TransportDisplayNumber? = null, // TODO get value from database in domain service

  /**
   * The party client ordering the transport.
   */
  val consignorParty: CompanyId,

  /**
   * The party executing the transport.
   */
  val carrierParty: CompanyId,

  val pickupLocation: Location,

  val pickupDateTime: Instant,

  val deliveryLocation: Location,

  val deliveryDateTime: Instant,

  val transportType: TransportType,

  val wasteContainer: WasteContainerId?,

  override val truck: LicensePlate?,

  override val driver: UserId?,

  val note: Note,

  override val transportHours: Duration?,

  val updatedAt: Instant?,

  /**
   * Used for ordering transports within the planning
   */
  val sequenceNumber: Int,

) : Transport {

  /**
   * Get the current status of this transport.
   * Delegates to the domain service for status calculation.
   */
  fun getStatus(): TransportStatus {
    return TransportStatusCalculator.calculateStatus(this)
  }
}
