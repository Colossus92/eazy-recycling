package nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket

import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.domain.model.declaration.LineDeclarationState
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import java.math.BigDecimal
import java.util.*

class WeightTicketLines(
  private val lines: List<WeightTicketLine>,
) {
  init {
    // Validate that all lines contain only MATERIAL or WASTE_STREAM catalog items
    lines.forEach { line ->
      require(line.catalogItemType == CatalogItemType.MATERIAL || line.catalogItemType == CatalogItemType.WASTE_STREAM) {
        "Sorteerwegingregels mogen alleen materialen of afvalstromen bevatten. Catalogusitem is van type ${line.catalogItemType}"
      }
    }
  }

  fun isEmpty() = lines.isEmpty()

  fun getLines() = lines
}

data class WeightTicketLine(
  val waste: WasteStreamNumber?,
  val catalogItemId: UUID,
  val catalogItemType: CatalogItemType,
  val weight: Weight,
  val declarationState: LineDeclarationState = LineDeclarationState.undeclared(),
)

class WeightTicketProductLines(
  private val lines: List<WeightTicketProductLine>,
) {
  init {
    // Validate that all product lines contain only PRODUCT catalog items
    lines.forEach { line ->
      require(line.catalogItemType == CatalogItemType.PRODUCT) {
        "Productregels mogen alleen producten bevatten. Catalogusitem is van type ${line.catalogItemType}"
      }
    }
  }

  fun isEmpty() = lines.isEmpty()

  fun getLines() = lines
}

data class WeightTicketProductLine(
  val catalogItemId: UUID,
  val catalogItemType: CatalogItemType,
  val quantity: BigDecimal,
  val unit: String,
)
