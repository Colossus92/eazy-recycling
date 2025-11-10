package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStream

interface FirstReceivalDeclarator {

  fun declareFirstReceivals(receivalDeclarations: List<ReceivalDeclaration>)
}

data class ReceivalDeclaration(
  val wasteStream: WasteStream,
  val transporters: List<String>,
  val totalWeight: Int,
  val totalShipments: Short,
  val yearMonth: YearMonth
)
