package nl.eazysoftware.eazyrecyclingservice.domain.model.transport

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainerId
import kotlin.time.Duration
import kotlin.time.Instant

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

  val deliveryDateTime: Instant?,

  val transportType: TransportType,

  val wasteContainer: WasteContainerId?,

  val containerOperation: ContainerOperation?,

  override val truck: LicensePlate?,

  override val driver: UserId?,

  /**
   * Any notes related to the transport. Typically noted by a planner.
   */
  val note: Note?,

  override val transportHours: Duration?,

  /**
   * Any notes the driver has, after the transport completed.
   */
  val driverNote: Note?,


  /**
   * Used for ordering transports within the planning
   */
  val sequenceNumber: Int,

  val createdAt: Instant? = null,
  val createdBy: String? = null,
  val updatedAt: Instant? = null,
  val updatedBy: String? = null,
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
      deliveryDateTime: Instant?,
      transportType: TransportType,
      wasteContainer: WasteContainerId?,
      containerOperation: ContainerOperation?,
      truck: LicensePlate?,
      driver: UserId?,
      note: Note,
      transportHours: Duration?,
      driverNote: Note?,
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
        driverNote = driverNote,
        updatedAt = updatedAt,
        sequenceNumber = sequenceNumber
      )
    }
  }
}
