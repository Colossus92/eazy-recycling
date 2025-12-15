package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import nl.eazysoftware.eazyrecyclingservice.application.query.CatalogQueryService
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItem
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/catalog")
@PreAuthorize(HAS_ANY_ROLE)
class CatalogController(
    private val catalogQueryService: CatalogQueryService
) {

    @GetMapping("/items")
    fun searchCatalogItems(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) itemTypes: Set<CatalogItemType>?,
        @RequestParam(required = false, defaultValue = "50") limit: Int
    ): List<CatalogItemResponse> {
        return catalogQueryService.searchCatalogItems(query, itemTypes, limit)
            .map { it.toResponse() }
    }

    @GetMapping("/items/{itemType}/{id}")
    fun getCatalogItemByTypeAndId(
        @PathVariable itemType: CatalogItemType,
        @PathVariable id: Long
    ): CatalogItemResponse {
        val item = catalogQueryService.getCatalogItemByTypeAndId(itemType, id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Catalog item not found")
        return item.toResponse()
    }

    @GetMapping("/materials/{id}")
    fun getMaterialById(@PathVariable id: Long): CatalogItemResponse {
        val item = catalogQueryService.getMaterialById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Material not found")
        return item.toResponse()
    }

    @GetMapping("/products/{id}")
    fun getProductById(@PathVariable id: Long): CatalogItemResponse {
        val item = catalogQueryService.getProductById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")
        return item.toResponse()
    }

    @GetMapping("/items/for-weight-ticket")
    fun getCatalogItemsForWeightTicket(
        @RequestParam consignorPartyId: UUID,
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) itemTypes: Set<CatalogItemType>?,
        @RequestParam(required = false, defaultValue = "50") limit: Int
    ): List<CatalogItemResponse> {
        return catalogQueryService.getCatalogItemsForWeightTicket(consignorPartyId, query, itemTypes, limit)
            .map { it.toResponse() }
    }
}

data class CatalogItemResponse(
    val id: Long,
    val code: String,
    val name: String,
    val unitOfMeasure: String,
    val vatCode: String,
    val glAccountCode: String?,
    val categoryName: String?,
    val itemType: CatalogItemType,
    val materialGroupId: Long?,
    val productCategoryId: Long?,
    val defaultPrice: BigDecimal?,
    val wasteStreamNumber: String?,
    val materialId: Long?,
    val consignorPartyId: String?,
    val euralCode: String?,
    val processingMethodCode: String?,
)

fun CatalogItem.toResponse(): CatalogItemResponse {
    return when (this) {
        is CatalogItem.MaterialItem -> CatalogItemResponse(
            id = id,
            code = code,
            name = name,
            unitOfMeasure = unitOfMeasure,
            vatCode = vatCode,
            glAccountCode = glAccountCode,
            categoryName = categoryName,
            itemType = itemType,
            materialGroupId = materialGroupId,
            productCategoryId = null,
            defaultPrice = null,
            wasteStreamNumber = null,
            materialId = null,
            consignorPartyId = null,
            euralCode = null,
            processingMethodCode = null,
        )
        is CatalogItem.ProductItem -> CatalogItemResponse(
            id = id,
            code = code,
            name = name,
            unitOfMeasure = unitOfMeasure,
            vatCode = vatCode,
            glAccountCode = glAccountCode,
            categoryName = categoryName,
            itemType = itemType,
            materialGroupId = null,
            productCategoryId = productCategoryId,
            defaultPrice = defaultPrice,
            wasteStreamNumber = null,
            materialId = null,
            consignorPartyId = null,
            euralCode = null,
            processingMethodCode = null,
        )
        is CatalogItem.WasteStreamItem -> CatalogItemResponse(
            id = id,
            code = code,
            name = name,
            unitOfMeasure = unitOfMeasure,
            vatCode = vatCode,
            glAccountCode = glAccountCode,
            categoryName = categoryName,
            itemType = itemType,
            materialGroupId = null,
            productCategoryId = null,
            defaultPrice = null,
            wasteStreamNumber = wasteStreamNumber,
            materialId = materialId,
            consignorPartyId = consignorPartyId,
            euralCode = euralCode,
            processingMethodCode = processingMethodCode,
        )
    }
}
