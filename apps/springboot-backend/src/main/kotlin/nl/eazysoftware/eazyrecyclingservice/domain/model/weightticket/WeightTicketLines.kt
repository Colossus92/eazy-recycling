package nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket

import nl.eazysoftware.eazyrecyclingservice.domain.model.declaration.LineDeclarationState
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import java.util.*

class WeightTicketLines(
  private val lines: List<WeightTicketLine>,
  ) {
  fun isEmpty() = lines.isEmpty()

  fun getLines() = lines
}


data class WeightTicketLine(
  val waste: WasteStreamNumber?,
  val catalogItemId: UUID,
  val weight: Weight,
  val declarationState: LineDeclarationState = LineDeclarationState.undeclared(),
)
