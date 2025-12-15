package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItem
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.product.Product
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Materials
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Products
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialQueryResult
import org.springframework.stereotype.Service
import java.util.*

@Service
class CatalogQueryService(
    private val materials: Materials,
    private val products: Products,
    private val wasteStreams: WasteStreams,
) {

    fun searchCatalogItems(
        query: String?,
        itemTypes: Set<CatalogItemType>? = null,
        limit: Int = 50
    ): List<CatalogItem> {
        val results = mutableListOf<CatalogItem>()

        // Include materials if not filtered out
        if (itemTypes == null || itemTypes.contains(CatalogItemType.MATERIAL)) {
            val materialResults = if (query.isNullOrBlank()) {
                materials.getAllMaterialsWithGroupDetails()
            } else {
                materials.searchMaterials(query, limit)
            }
            results.addAll(materialResults.map { it.toCatalogItem() })
        }

        // Include products if not filtered out
        if (itemTypes == null || itemTypes.contains(CatalogItemType.PRODUCT)) {
            val productResults = if (query.isNullOrBlank()) {
                products.getActiveProducts()
            } else {
                products.searchProducts(query, limit)
            }
            results.addAll(productResults.map { it.toCatalogItem() })
        }

        // Sort combined results by name and apply limit
        return results
            .sortedBy { it.name }
            .take(limit)
    }

    fun getMaterialById(id: Long): CatalogItem.MaterialItem? {
        return materials.getMaterialWithGroupDetailsById(id)?.toCatalogItem()
    }

    fun getProductById(id: Long): CatalogItem.ProductItem? {
        return products.getProductById(id)?.toCatalogItem()
    }

    fun getCatalogItemByTypeAndId(itemType: CatalogItemType, id: Long): CatalogItem? {
        return when (itemType) {
            CatalogItemType.MATERIAL -> getMaterialById(id)
            CatalogItemType.PRODUCT -> getProductById(id)
            CatalogItemType.WASTE_STREAM -> null // Waste streams are looked up by number, not id
        }
    }

    /**
     * Gets catalog items for weight ticket lines.
     * Returns:
     * - Waste streams for the given consignor (filtered by consignorPartyId)
     * - Products (available to all)
     * - Materials (available to all)
     */
    fun getCatalogItemsForWeightTicket(
        consignorPartyId: UUID,
        query: String?,
        itemTypes: Set<CatalogItemType>? = null,
        limit: Int = 50
    ): List<CatalogItem> {
        val results = mutableListOf<CatalogItem>()
        val companyId = CompanyId(consignorPartyId)

        // Include waste streams for the consignor if not filtered out
        if (itemTypes == null || itemTypes.contains(CatalogItemType.WASTE_STREAM)) {
            val wasteStreamResults = wasteStreams.findActiveByConsignorPartyId(companyId)
            val filteredWasteStreams = if (query.isNullOrBlank()) {
                wasteStreamResults
            } else {
                wasteStreamResults.filter {
                    it.wasteType.name.contains(query, ignoreCase = true) ||
                    it.wasteStreamNumber.number.contains(query, ignoreCase = true)
                }
            }
            results.addAll(filteredWasteStreams.map { it.toCatalogItem() })
        }

        // Include materials if not filtered out
        if (itemTypes == null || itemTypes.contains(CatalogItemType.MATERIAL)) {
            val materialResults = if (query.isNullOrBlank()) {
                materials.getAllMaterialsWithGroupDetails()
            } else {
                materials.searchMaterials(query, limit)
            }
            results.addAll(materialResults.map { it.toCatalogItem() })
        }

        // Include products if not filtered out
        if (itemTypes == null || itemTypes.contains(CatalogItemType.PRODUCT)) {
            val productResults = if (query.isNullOrBlank()) {
                products.getActiveProducts()
            } else {
                products.searchProducts(query, limit)
            }
            results.addAll(productResults.map { it.toCatalogItem() })
        }

        // Sort combined results by name and apply limit
        return results
            .sortedBy { it.name }
            .take(limit)
    }
}

private fun MaterialQueryResult.toCatalogItem(): CatalogItem.MaterialItem {
    return CatalogItem.MaterialItem(
        id = getId(),
        code = getCode(),
        name = getName(),
        unitOfMeasure = getUnitOfMeasure(),
        vatCode = getVatCode(),
        glAccountCode = getSalesAccountNumber(),
        categoryName = getMaterialGroupName(),
        materialGroupId = getMaterialGroupId(),
    )
}

private fun Product.toCatalogItem(): CatalogItem.ProductItem {
    return CatalogItem.ProductItem(
        id = id!!,
        code = code,
        name = name,
        unitOfMeasure = unitOfMeasure,
        vatCode = vatCode,
        glAccountCode = glAccountCode,
        categoryName = categoryName,
        productCategoryId = categoryId,
        defaultPrice = defaultPrice,
    )
}

private fun WasteStream.toCatalogItem(): CatalogItem.WasteStreamItem {
    val consignorId = when (val consignor = consignorParty) {
        is nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor.Company -> consignor.id.uuid.toString()
        is nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor.Person -> "PERSON"
    }
    return CatalogItem.WasteStreamItem(
        id = catalogItemId ?: 0L,
        code = wasteStreamNumber.number,
        name = wasteType.name,
        unitOfMeasure = "kg",
        vatCode = "HOOG",
        glAccountCode = null,
        categoryName = null,
        wasteStreamNumber = wasteStreamNumber.number,
        materialId = catalogItemId,
        consignorPartyId = consignorId,
        euralCode = wasteType.euralCode.code,
        processingMethodCode = wasteType.processingMethod.code,
    )
}
