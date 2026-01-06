package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStream
import org.springframework.stereotype.Service

/**
 * Domain service for checking waste stream compatibility.
 * Used to determine if multiple waste streams can be combined in a single transport.
 *
 * Two waste streams are compatible if they have:
 * - The same pickup location
 * - The same delivery location (processor party)
 * - The same consignor party
 */
@Service
class WasteStreamCompatibilityService {

  /**
   * Checks if a waste stream is compatible with a reference waste stream.
   *
   * @param wasteStream The waste stream to check
   * @param referenceStream The reference waste stream to compare against
   * @return true if both waste streams are compatible, false otherwise
   */
  fun isCompatibleWith(wasteStream: WasteStream, referenceStream: WasteStream): Boolean {
    return wasteStream.pickupLocation == referenceStream.pickupLocation &&
      wasteStream.deliveryLocation.processorPartyId == referenceStream.deliveryLocation.processorPartyId &&
      wasteStream.consignorParty == referenceStream.consignorParty
  }

  /**
   * Checks if multiple waste streams are compatible with each other for combining in a single transport.
   * All waste streams must have the same pickup location, delivery location, and consignor party.
   *
   * @param wasteStreams The list of waste streams to check for compatibility
   * @return true if all waste streams are compatible with each other, false otherwise
   * @throws IllegalArgumentException if the list is empty
   */
  fun areCompatible(wasteStreams: List<WasteStream>): Boolean {
    require(wasteStreams.isNotEmpty()) {
      "Minstens één afvalstroomnummer is vereist om compatibiliteit te controleren"
    }

    if (wasteStreams.size == 1) {
      return true
    }

    val firstStream = wasteStreams.first()

    // Check that all waste streams have the same:
    // 1. Pickup location
    // 2. Delivery location (processor party)
    // 3. Consignor party
    return wasteStreams.all { stream -> isCompatibleWith(stream, firstStream)
    }
  }

  /**
   * Gets a detailed incompatibility reason if waste streams are not compatible.
   * Useful for providing feedback to the user about why waste streams cannot be combined.
   *
   * @param wasteStreams The list of waste streams to check
   * @return A string describing the incompatibility reason, or null if they are compatible
   */
  fun getIncompatibilityReason(wasteStreams: List<WasteStream>): String? {
    if (wasteStreams.isEmpty() || wasteStreams.size == 1) {
      return null
    }

    val firstStream = wasteStreams.first()

    for (stream in wasteStreams.drop(1)) {
      when {
        stream.pickupLocation != firstStream.pickupLocation ->
          return "Afvalstromen hebben verschillende ophaallocaties"

        stream.deliveryLocation.processorPartyId != firstStream.deliveryLocation.processorPartyId ->
          return "Afvalstromen hebben verschillende ontvangende verwerkers"

        stream.consignorParty != firstStream.consignorParty ->
          return "Afvalstromen hebben verschillende afzenders"
      }
    }

    return null
  }
}
