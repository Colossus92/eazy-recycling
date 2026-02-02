package nl.eazysoftware.eazyrecyclingservice.domain.model.catalog

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import java.math.BigDecimal
import java.util.*

data class CatalogItem (
  val id: UUID,
  val type: CatalogItemType,
  val code: String,
  val name: String,
  val unitOfMeasure: String,
  val vatCode: String,
  val vatPercentage: BigDecimal?,
  val categoryName: String?,
  val consignorPartyId: CompanyId?,
  val defaultPrice: BigDecimal?,
  val purchaseAccountNumber: String?,
  val salesAccountNumber: String?,
  val wasteStreamNumber: WasteStreamNumber?,
  val itemType: CatalogItemType,
  // Pickup location address fields (for waste streams)
  val pickupStreet: String? = null,
  val pickupBuildingNumber: String? = null,
  val pickupCity: String? = null,
)

enum class CatalogItemType {
    MATERIAL,
    PRODUCT,
    WASTE_STREAM
}
