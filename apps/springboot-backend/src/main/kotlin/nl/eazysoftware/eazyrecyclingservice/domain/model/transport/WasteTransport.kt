package nl.eazysoftware.eazyrecyclingservice.domain.model.transport

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainerId
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

  val pickupDateTime: Instant,

  val deliveryDateTime: Instant?,

  val transportType: TransportType = TransportType.WASTE,

  val goods: List<GoodsItem>,

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
      pickupDateTime: Instant,
      deliveryDateTime: Instant,
      transportType: TransportType = TransportType.WASTE,
      goodsItem: List<GoodsItem>,
      wasteContainer: WasteContainerId?,
      containerOperation: ContainerOperation?,
      truck: LicensePlate?,
      driver: UserId?,
      note: Note,
      transportHours: Duration?,
      updatedAt: Instant?,
      sequenceNumber: Int,
    ): WasteTransport {
      return WasteTransport(
        transportId = TransportId.generate(),
        displayNumber = displayNumber,
        carrierParty = carrierParty,
        pickupDateTime = pickupDateTime,
        deliveryDateTime = deliveryDateTime,
        transportType = transportType,
        goods = goodsItem,
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

data class GoodsItem(
  val wasteStreamNumber: WasteStreamNumber,
  val netNetWeight: Double,
  val unit: String,
  val quantity: Int,
)
