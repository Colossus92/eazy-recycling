package nl.eazysoftware.eazyrecyclingservice.domain.waste

data class Goods(
  val id: Long,
  val waste: WasteStream,
  val weight: Weight,
) {
}
