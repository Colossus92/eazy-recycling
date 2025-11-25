package nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket

import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight

class WeightTicketLines(
  private val lines: List<WeightTicketLine>,
  ) {
  fun isEmpty() = lines.isEmpty()

  fun getLines() = lines
}


data class WeightTicketLine(
  val waste: WasteStreamNumber,
  val weight: Weight,
)
