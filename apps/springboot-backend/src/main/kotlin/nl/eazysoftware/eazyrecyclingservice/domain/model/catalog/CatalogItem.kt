package nl.eazysoftware.eazyrecyclingservice.domain.model.catalog

import java.math.BigDecimal

sealed class CatalogItem {
    abstract val id: Long
    abstract val code: String
    abstract val name: String
    abstract val unitOfMeasure: String
    abstract val vatCode: String
    abstract val glAccountCode: String?
    abstract val categoryName: String?
    abstract val itemType: CatalogItemType

    data class MaterialItem(
        override val id: Long,
        override val code: String,
        override val name: String,
        override val unitOfMeasure: String,
        override val vatCode: String,
        override val glAccountCode: String?,
        override val categoryName: String?,
        val materialGroupId: Long?,
    ) : CatalogItem() {
        override val itemType = CatalogItemType.MATERIAL
    }

    data class ProductItem(
        override val id: Long,
        override val code: String,
        override val name: String,
        override val unitOfMeasure: String,
        override val vatCode: String,
        override val glAccountCode: String?,
        override val categoryName: String?,
        val productCategoryId: Long?,
        val defaultPrice: BigDecimal?,
    ) : CatalogItem() {
        override val itemType = CatalogItemType.PRODUCT
    }

    data class WasteStreamItem(
        override val id: Long,
        override val code: String,
        override val name: String,
        override val unitOfMeasure: String,
        override val vatCode: String,
        override val glAccountCode: String?,
        override val categoryName: String?,
        val wasteStreamNumber: String,
        val materialId: Long?,
        val consignorPartyId: String,
        val euralCode: String,
        val processingMethodCode: String,
    ) : CatalogItem() {
        override val itemType = CatalogItemType.WASTE_STREAM
    }
}

enum class CatalogItemType {
    MATERIAL,
    PRODUCT,
    WASTE_STREAM
}
