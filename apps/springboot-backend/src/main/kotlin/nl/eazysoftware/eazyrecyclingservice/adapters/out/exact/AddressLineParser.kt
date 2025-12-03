package nl.eazysoftware.eazyrecyclingservice.adapters.out.exact

/**
 * Utility class for parsing address lines from Exact Online.
 */
object AddressLineParser {

  /**
   * Parse address line into street name, building number, and addition.
   * 
   * Supported formats:
   * - "Street Name 123" -> ("Street Name", "123", null)
   * - "Street Name 123A" -> ("Street Name", "123", "A")
   * - "Street Name 123 A" -> ("Street Name", "123", "A")
   * - "Street Name" -> ("Street Name", "", null)
   * - null or blank -> ("", "", null)
   * 
   * @param addressLine The address line to parse
   * @return Triple of (streetName, buildingNumber, buildingNumberAddition)
   */
  fun parse(addressLine: String?): Triple<String, String, String?> {
    if (addressLine.isNullOrBlank()) {
      return Triple("", "", null)
    }

    // Regex to match: street name, building number, optional space, optional addition
    // Group 1: Street name (non-greedy match of everything before the number)
    // Group 2: Building number (one or more digits)
    // Group 3: Optional addition (letters, possibly preceded by a space)
    val regex = Regex("""^(.+?)\s+(\d+)\s*([A-Za-z]*)$""")
    val match = regex.find(addressLine.trim())

    return if (match != null) {
      val (street, number, addition) = match.destructured
      Triple(street, number, addition.ifBlank { null })
    } else {
      // If no match, treat the whole thing as street name
      Triple(addressLine.trim(), "", null)
    }
  }
}
