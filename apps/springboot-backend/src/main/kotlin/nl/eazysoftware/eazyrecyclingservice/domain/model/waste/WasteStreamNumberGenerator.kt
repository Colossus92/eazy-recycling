package nl.eazysoftware.eazyrecyclingservice.domain.model.waste

import nl.eazysoftware.eazyrecyclingservice.domain.model.Tenant
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreamSequences
import org.springframework.stereotype.Component

/**
 * Domain service responsible for generating sequential waste stream numbers
 * for the current tenant only.
 *
 * Format: 12 digits total
 * - First 5 digits: processorId (from current Tenant)
 * - Last 7 digits: sequential number (0000001 - 9999999)
 *
 * Example: For tenant processorId "08797"
 * - First number:  087970000001
 * - Second number: 087970000002
 * - etc.
 *
 * The sequence is managed in the database, allowing the tenant to have
 * their own starting value to avoid collisions with external systems.
 */
@Component
class WasteStreamNumberGenerator(
  private val sequences: WasteStreamSequences
) {

  /**
   * Generates the next waste stream number for the current tenant.
   * Uses a database sequence to ensure thread-safety and persistence.
   *
   * @return A new WasteStreamNumber with the next sequential value
   */
  fun generateNext(): WasteStreamNumber {
    val processorId = Tenant.processorPartyId
    val nextSequentialNumber = sequences.nextValue(processorId)

    require(nextSequentialNumber <= 9999999L) {
      "Maximum aantal afvalstroomnummers bereikt voor verwerker ${processorId.number}"
    }

    // Format: processorId (5 digits) + sequential number (7 digits, zero-padded)
    val wasteStreamNumber = "${processorId.number}${nextSequentialNumber.toString().padStart(7, '0')}"

    return WasteStreamNumber(wasteStreamNumber)
  }
}
