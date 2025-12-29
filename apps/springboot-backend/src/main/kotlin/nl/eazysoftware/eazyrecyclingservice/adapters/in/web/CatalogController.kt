package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import nl.eazysoftware.eazyrecyclingservice.application.query.CatalogQueryService
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItem
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
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
  ): List<CatalogItemResponse> {
    return catalogQueryService.getCatalogItems(consignorPartyId, query)
      .map { it.toResponse() }
  }
}

data class CatalogItemResponse(
  val id: UUID,
  val itemType: CatalogItemType,
  val code: String,
  val name: String,
  val unitOfMeasure: String,
  val vatCode: String,
  val categoryName: String?,
  val consignorPartyId: UUID?,
  val purchaseAccountNumber: String?,
  val salesAccountNumber: String?,
  val wasteStreamNumber: String?,
  val defaultPrice: BigDecimal?,
)

fun CatalogItem.toResponse() = CatalogItemResponse(
  id = id,
  itemType = itemType,
  code = code,
  name = name,
  unitOfMeasure = unitOfMeasure,
  vatCode = vatCode,
  categoryName = categoryName,
  consignorPartyId = consignorPartyId?.uuid,
  purchaseAccountNumber = purchaseAccountNumber,
  salesAccountNumber = salesAccountNumber,
  wasteStreamNumber = wasteStreamNumber?.number,
  defaultPrice = defaultPrice,
)
