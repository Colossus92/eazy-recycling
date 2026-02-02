package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import nl.eazysoftware.eazyrecyclingservice.application.query.CatalogQueryService
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItem
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.*

@RestController
@RequestMapping("/catalog")
@PreAuthorize(HAS_ANY_ROLE)
class CatalogController(
  private val catalogQueryService: CatalogQueryService
) {

  @GetMapping("/items")
  fun searchCatalogItems(
    @RequestParam(required = false) query: String?,
    @RequestParam(required = false) consignorPartyId: UUID?,
    @RequestParam(required = false) type: CatalogItemType?,
    @RequestParam(required = false) invoiceType: InvoiceType?,
  ): List<CatalogItemResponse> {
    return catalogQueryService.getCatalogItems(consignorPartyId, query)
      .filter { if (type == null) true else it.itemType == type }
      .map { it.toResponse(invoiceType) }
  }
}

data class CatalogItemResponse(
  /** Unique identifier: catalog item UUID for MATERIAL/PRODUCT, waste stream number for WASTE_STREAM */
  val id: String,
  val itemType: CatalogItemType,
  /** The catalog item UUID used for billing/pricing (same as id for MATERIAL/PRODUCT) */
  val catalogItemId: UUID,
  val code: String,
  val name: String,
  val unitOfMeasure: String,
  val vatCode: String,
  val vatPercentage: BigDecimal?,
  val categoryName: String?,
  val consignorPartyId: UUID?,
  val purchaseAccountNumber: String?,
  val salesAccountNumber: String?,
  val wasteStreamNumber: String?,
  val defaultPrice: BigDecimal?,
  // Pickup location address fields (for waste streams)
  val pickupStreet: String?,
  val pickupBuildingNumber: String?,
  val pickupCity: String?,
)

fun CatalogItem.toResponse(invoiceType: InvoiceType? = null): CatalogItemResponse {
  // For purchase invoices, only PRODUCT prices should be negative (money owed TO the supplier)
  val priceMultiplier = if (invoiceType == InvoiceType.PURCHASE && itemType == CatalogItemType.PRODUCT) 
    BigDecimal.ONE.negate() 
  else 
    BigDecimal.ONE

  return CatalogItemResponse(
    // For WASTE_STREAM, use the waste stream number as unique ID; otherwise use catalog item UUID
    id = if (itemType == CatalogItemType.WASTE_STREAM && wasteStreamNumber != null) 
           wasteStreamNumber.number 
         else 
           id.toString(),
    itemType = itemType,
    catalogItemId = id,  // Always the catalog item UUID for billing
    code = code,
    name = name,
    unitOfMeasure = unitOfMeasure,
    vatCode = vatCode,
    vatPercentage = vatPercentage,
    categoryName = categoryName,
    consignorPartyId = consignorPartyId?.uuid,
    purchaseAccountNumber = purchaseAccountNumber,
    salesAccountNumber = salesAccountNumber,
    wasteStreamNumber = wasteStreamNumber?.number,
    defaultPrice = defaultPrice?.multiply(priceMultiplier),
    pickupStreet = pickupStreet,
    pickupBuildingNumber = pickupBuildingNumber,
    pickupCity = pickupCity,
  )
}
