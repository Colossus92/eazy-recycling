package nl.eazysoftware.eazyrecyclingservice.domain.model.waste

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId

/**
 * Domain service responsible for generating sequential waste stream numbers.
 *
 * Format: 12 digits total
 * - First 5 digits: processorId (from ProcessorPartyId)
 * - Last 7 digits: sequential number (0000001 - 9999999)
 *
 * Example: For processorId "12345"
 * - First number:  123450000001
 * - Second number: 123450000002
 * - etc.
 */
class WasteStreamNumberGenerator {

  /**
   * Generates the next waste stream number for a given processor.
   *
   * @param processorId The 5-digit processor identification
   * @param highestExistingNumber The highest sequential number already in use for this processor (null if none exist)
   * @return A new WasteStreamNumber with the next sequential value
   */
  fun generateNext(
    processorId: ProcessorPartyId,
    highestExistingNumber: WasteStreamNumber?
  ): WasteStreamNumber {
    val nextSequentialNumber = if (highestExistingNumber == null) {
      1L
    } else {
      // Extract the last 7 digits from the existing number
      val sequentialPart = highestExistingNumber.number.substring(5).toLong()
      sequentialPart + 1
    }

    require(nextSequentialNumber <= 9999999L) {
      "Maximum aantal afvalstroomnummers bereikt voor verwerker ${processorId.number}"
    }

    // Format: processorId (5 digits) + sequential number (7 digits, zero-padded)
    val wasteStreamNumber = "${processorId.number}${nextSequentialNumber.toString().padStart(7, '0')}"

    return WasteStreamNumber(wasteStreamNumber)
  }
}
