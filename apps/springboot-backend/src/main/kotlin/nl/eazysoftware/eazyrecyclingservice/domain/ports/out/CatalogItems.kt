package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItem
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import java.util.*

interface CatalogItems {

  fun findAll(consignorPartyId: UUID?, query: String?): List<CatalogItem>

  /**
   * Find all catalog items of a specific type (for fuzzy matching)
   */
  fun findAllByType(type: CatalogItemType): List<CatalogItem>

  /**
   * Create a new catalog item
   */
  fun create(catalogItem: CatalogItem): CatalogItem
}
