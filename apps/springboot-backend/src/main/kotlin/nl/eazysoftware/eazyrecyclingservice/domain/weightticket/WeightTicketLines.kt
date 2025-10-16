package nl.eazysoftware.eazyrecyclingservice.domain.weightticket

import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.waste.Weight

class WeightTicketLines(
  private val lines: List<WeightTicketLine>,
  ) {
  fun isEmpty() = lines.isEmpty()

  fun getLines() = lines.toList()
}


data class WeightTicketLine(
  val waste: WasteStreamNumber,
  val weight: Weight,
)
