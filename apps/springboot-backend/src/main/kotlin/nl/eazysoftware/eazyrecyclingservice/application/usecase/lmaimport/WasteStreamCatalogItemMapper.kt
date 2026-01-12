package nl.eazysoftware.eazyrecyclingservice.application.usecase.lmaimport

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Hard-coded mapping between LMA waste stream names and catalog item names.
 * This is a temporary solution to map imported waste streams to existing catalog items.
 * 
 * TODO: Remove this mapper when catalog items are properly managed through the UI.
 */
@Component
class WasteStreamCatalogItemMapper {
  private val logger = LoggerFactory.getLogger(javaClass)

  companion object {
    /**
     * Maps waste stream name (case-insensitive) to catalog item name.
     * Returns null if no mapping exists.
     */
    private val MAPPINGS = mapOf(
      "ijzer en staal" to "Diverse metalen",
      "aluminium" to "Aluminium",
      "gemengde metalen" to "Diverse metalen",
      "koper, brons en messing" to "Messing",
      "loodaccus" to "Accu lood",
      "metalen" to "Diverse metalen"
    )
  }

  /**
   * Maps a waste stream name to a catalog item name using hard-coded mappings.
   * 
   * Mapping rules (case-insensitive):
   * - "ijzer en staal" -> "Diverse metalen"
   * - "aluminium" -> "Aluminium"
   * - "gemengde metalen" -> "Diverse metalen"
   * - "koper, brons en messing" -> "Messing"
   * - "loodaccus" -> "Accu lood"
   * - "metalen" -> "Diverse metalen"
   * - Contains "kabels" -> "Diverse soorten kabel"
   * 
   * @param wasteStreamName The name of the waste stream from LMA import
   * @return The mapped catalog item name, or null if no mapping exists
   */
  fun mapToCatalogItemName(wasteStreamName: String): String? {
    val normalizedName = wasteStreamName.trim().lowercase()
    
    // Check for special case: contains "kabels"
    if (normalizedName.contains("kabels")) {
      logger.debug("Mapped '$wasteStreamName' to 'Diverse soorten kabel' (contains 'kabels')")
      return "Diverse soorten kabel"
    }
    
    // Check exact mappings
    val mapped = MAPPINGS[normalizedName]
    if (mapped != null) {
      logger.debug("Mapped '$wasteStreamName' to '$mapped'")
    } else {
      logger.debug("No mapping found for '$wasteStreamName'")
    }
    
    return mapped
  }
}
