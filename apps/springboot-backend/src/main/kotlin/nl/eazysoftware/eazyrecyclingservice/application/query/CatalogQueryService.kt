package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItem
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.CatalogItems
import org.springframework.stereotype.Service
import java.util.*

@Service
class CatalogQueryService(
    private val catalogItems: CatalogItems,
) {

    /**
     * Gets catalog items for weight ticket lines.
     * Returns:
     * - Waste streams for the given consignor (if consignorPartyId provided)
     * - Generic catalog items (materials and services available to all)
     */
    fun getCatalogItems(
        consignorPartyId: UUID?,
        query: String?,
    ): List<CatalogItem> {
        return catalogItems.findAll(consignorPartyId, query)
            .sortedBy { it.name }
    }
}
