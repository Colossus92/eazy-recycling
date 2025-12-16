package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItem
import java.util.UUID

interface CatalogItems {

  fun findAll(consignorPartyId: UUID?, query: String?): List<CatalogItem>
}
