package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItem
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.domain.model.product.Product
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Materials
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Products
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialQueryResult
import org.springframework.stereotype.Service

@Service
class CatalogQueryService(
    private val materials: Materials,
    private val products: Products,
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
        }
    }
}

private fun MaterialQueryResult.toCatalogItem(): CatalogItem.MaterialItem {
    return CatalogItem.MaterialItem(
        id = getId(),
        code = getCode(),
        name = getName(),
        unitOfMeasure = getUnitOfMeasure(),
        vatCode = getVatCode(),
        glAccountCode = getGlAccountCode(),
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
