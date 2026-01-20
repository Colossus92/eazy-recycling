package nl.eazysoftware.eazyrecyclingservice.domain.model.transport

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.service.TransportDisplayNumberGenerator
import nl.eazysoftware.eazyrecyclingservice.domain.service.WasteStreamCompatibilityService
import org.springframework.stereotype.Component
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Factory for creating WasteTransport aggregates.
 *
 * This factory enforces the business rule that waste streams in a transport must be compatible.
 * By making this the ONLY way to create a WasteTransport (constructor is internal),
 * we guarantee the invariant is always checked.
 *
 * This follows the DDD pattern where:
 * - The aggregate enforces invariants over data it OWNS
 * - The factory enforces invariants that require external data/services
 */
@Component
class WasteTransportFactory(
  private val compatibilityService: WasteStreamCompatibilityService,
  private val transportDisplayNumberGenerator: TransportDisplayNumberGenerator
) {

  /**
   * Creates a new WasteTransport with validated waste stream compatibility.
   *
   * @param wasteStreams The actual WasteStream aggregates (not just references)
   * @param goods The goods items corresponding to the waste streams
   * @throws IllegalArgumentException if waste streams are not compatible
   */
  fun create(
    carrierParty: CompanyId,
    pickupTimingConstraint: TimingConstraint? = null,
    deliveryTimingConstraint: TimingConstraint? = null,
    wasteStreams: List<WasteStream>,
    goods: List<GoodsItem>,
    wasteContainer: WasteContainerId?,
    containerOperation: ContainerOperation?,
    truck: LicensePlate?,
    driver: UserId?,
    note: Note?,
    transportHours: Duration?,
    driverNote: Note?,
    sequenceNumber: Int,
    weightTicketId: WeightTicketId? = null,
  ): WasteTransport {
    validate(goods, wasteStreams)

    val displayNumber = transportDisplayNumberGenerator.generateDisplayNumber()

    // All validations passed - create the aggregate
    return WasteTransport(
      transportId = TransportId.generate(),
      displayNumber = displayNumber,
      carrierParty = carrierParty,
      pickupTimingConstraint = pickupTimingConstraint,
      deliveryTimingConstraint = deliveryTimingConstraint,
      transportType = TransportType.WASTE,
      goods = goods,
      wasteContainer = wasteContainer,
      containerOperation = containerOperation,
      truck = truck,
      driver = driver,
      note = note,
      transportHours = transportHours,
      driverNote = driverNote,
      updatedAt = Instant.DISTANT_PAST,
      sequenceNumber = sequenceNumber,
      weightTicketId = weightTicketId,
    )
  }

  /**
   * Updates an existing WasteTransport with new goods items.
   * Validates that the new waste streams are compatible.
   */
  fun update(
    existing: WasteTransport,
    wasteStreams: List<WasteStream>,
    goods: List<GoodsItem>,
    carrierParty: CompanyId = existing.carrierParty,
    pickupTimingConstraint: TimingConstraint? = existing.pickupTimingConstraint,
    deliveryTimingConstraint: TimingConstraint? = existing.deliveryTimingConstraint,
    wasteContainer: WasteContainerId? = existing.wasteContainer,
    containerOperation: ContainerOperation? = existing.containerOperation,
    truck: LicensePlate? = existing.truck,
    driver: UserId? = existing.driver,
    note: Note? = existing.note,
    weightTicketId: WeightTicketId? = null,
  ): WasteTransport {
    validate(goods, wasteStreams)

    return WasteTransport(
      transportId = existing.transportId,
      displayNumber = existing.displayNumber,
      carrierParty = carrierParty,
      pickupTimingConstraint = pickupTimingConstraint,
      deliveryTimingConstraint = deliveryTimingConstraint,
      transportType = existing.transportType,
      goods = goods,
      wasteContainer = wasteContainer,
      containerOperation = containerOperation,
      truck = truck,
      driver = driver,
      note = note,
      transportHours = existing.transportHours,
      driverNote = existing.driverNote,
      updatedAt = kotlin.time.Clock.System.now(),
      sequenceNumber = existing.sequenceNumber,
      weightTicketId = weightTicketId,
    )
  }

  private fun validate(
    goods: List<GoodsItem>,
    wasteStreams: List<WasteStream>
  ) {
    require(goods.map { it.wasteStreamNumber }.all { wasteStreamNumber -> wasteStreams.any { it.wasteStreamNumber == wasteStreamNumber } }) {
      "Missende afvalstromen bij de goederen"
    }

    // Validate compatibility using the domain service
    if (!compatibilityService.areCompatible(wasteStreams)) {
      val reason = compatibilityService.getIncompatibilityReason(wasteStreams)
      throw IncompatibleWasteStreamsException(
        "Afvalstromen kunnen niet worden gecombineerd in één transport: $reason"
      )
    }
  }
}

/**
 * Domain exception thrown when attempting to combine incompatible waste streams in a transport.
 */
class IncompatibleWasteStreamsException(message: String) : IllegalArgumentException(message)
