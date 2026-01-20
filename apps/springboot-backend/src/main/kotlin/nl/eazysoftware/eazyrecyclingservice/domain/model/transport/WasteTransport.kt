package nl.eazysoftware.eazyrecyclingservice.domain.model.transport

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import kotlin.time.Duration
import kotlin.time.Instant

class WasteTransport(
  val transportId: TransportId,

  /**
   * Used for human-readable display in the UI.
   * Generated automatically by TransportDisplayNumberGenerator for new transports.
   */
  val displayNumber: TransportDisplayNumber? = null,

  /**
   * The party executing the transport.
   */
  val carrierParty: CompanyId,

  /**
   * Timing constraint for pickup scheduling (DATE_ONLY, WINDOW, or FIXED).
   */
  val pickupTimingConstraint: TimingConstraint? = null,

  /**
   * Timing constraint for delivery scheduling (DATE_ONLY, WINDOW, or FIXED).
   */
  val deliveryTimingConstraint: TimingConstraint? = null,

  val transportType: TransportType = TransportType.WASTE,

  val goods: List<GoodsItem>,

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

  var weightTicketId: WeightTicketId? = null,

  val createdAt: Instant? = null,
  val createdBy: String? = null,
  val updatedAt: Instant? = null,
  val updatedBy: String? = null,

  ) : Transport {

  init {
      require(goods.isNotEmpty()) {
        "Een afvaltransport moet afval bevatten"
      }
  }

  /**
   * Get the current status of this transport.
   * Delegates to the domain service for status calculation.
   */
  fun getStatus(): TransportStatus {
    return TransportStatusCalculator.calculateStatus(this)
  }

  companion object {
    /**
     * Factory method to create a new WasteTransport with a generated UUID.
     */
    fun create(
      displayNumber: TransportDisplayNumber,
      carrierParty: CompanyId,
      pickupTimingConstraint: TimingConstraint? = null,
      deliveryTimingConstraint: TimingConstraint? = null,
      transportType: TransportType = TransportType.WASTE,
      goodsItem: List<GoodsItem>,
      wasteContainer: WasteContainerId?,
      containerOperation: ContainerOperation?,
      truck: LicensePlate?,
      driver: UserId?,
      note: Note,
      driverNote: Note?,
      transportHours: Duration?,
      updatedAt: Instant?,
      sequenceNumber: Int,
      weightTicketId: WeightTicketId? = null,
    ): WasteTransport {
      return WasteTransport(
        transportId = TransportId.generate(),
        displayNumber = displayNumber,
        carrierParty = carrierParty,
        pickupTimingConstraint = pickupTimingConstraint,
        deliveryTimingConstraint = deliveryTimingConstraint,
        transportType = transportType,
        goods = goodsItem,
        wasteContainer = wasteContainer,
        containerOperation = containerOperation,
        truck = truck,
        driver = driver,
        note = note,
        transportHours = transportHours,
        driverNote = driverNote,
        updatedAt = updatedAt,
        sequenceNumber = sequenceNumber,
        weightTicketId = weightTicketId,
      )
    }
  }
}

data class GoodsItem(
  val wasteStreamNumber: WasteStreamNumber,
  val netNetWeight: Double?,
  val unit: String,
  val quantity: Int,
)
