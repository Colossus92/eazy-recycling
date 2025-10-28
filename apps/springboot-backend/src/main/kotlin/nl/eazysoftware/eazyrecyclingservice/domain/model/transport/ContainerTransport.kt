package nl.eazysoftware.eazyrecyclingservice.domain.model.transport

import kotlinx.datetime.Instant
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import kotlin.time.Duration

class ContainerTransport(
  val transportId: TransportId,

  /**
   * Used for human-readable display in the UI.
   * Generated automatically by TransportDisplayNumberGenerator for new transports.
   */
  val displayNumber: TransportDisplayNumber? = null,

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

  val containerOperation: ContainerOperation?,

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

  companion object {
    /**
     * Factory method to create a new ContainerTransport with a generated UUID.
     */
    fun create(
      displayNumber: TransportDisplayNumber,
      consignorParty: CompanyId,
      carrierParty: CompanyId,
      pickupLocation: Location,
      pickupDateTime: Instant,
      deliveryLocation: Location,
      deliveryDateTime: Instant,
      transportType: TransportType,
      wasteContainer: WasteContainerId?,
      containerOperation: ContainerOperation?,
      truck: LicensePlate?,
      driver: UserId?,
      note: Note,
      transportHours: Duration?,
      updatedAt: Instant?,
      sequenceNumber: Int,
    ): ContainerTransport {
      return ContainerTransport(
        transportId = TransportId.generate(),
        displayNumber = displayNumber,
        consignorParty = consignorParty,
        carrierParty = carrierParty,
        pickupLocation = pickupLocation,
        pickupDateTime = pickupDateTime,
        deliveryLocation = deliveryLocation,
        deliveryDateTime = deliveryDateTime,
        transportType = transportType,
        wasteContainer = wasteContainer,
        containerOperation = containerOperation,
        truck = truck,
        driver = driver,
        note = note,
        transportHours = transportHours,
        updatedAt = updatedAt,
        sequenceNumber = sequenceNumber
      )
    }
  }
}
