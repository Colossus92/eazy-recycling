package nl.eazysoftware.eazyrecyclingservice.domain.model.catalog

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import java.math.BigDecimal

data class CatalogItem (
  val id: Long,
  val type: CatalogItemType,
  val code: String,
  val name: String,
  val unitOfMeasure: String,
  val vatCode: String,
  val categoryName: String?,
  val consignorPartyId: CompanyId?,
  val defaultPrice: BigDecimal?,
  val purchaseAccountNumber: String?,
  val salesAccountNumber: String?,
  val wasteStreamNumber: WasteStreamNumber?,
  val itemType: CatalogItemType,
)

enum class CatalogItemType {
    MATERIAL,
    PRODUCT,
    WASTE_STREAM
}
