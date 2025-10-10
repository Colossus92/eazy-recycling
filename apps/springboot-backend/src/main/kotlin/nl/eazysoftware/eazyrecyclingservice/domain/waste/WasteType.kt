package nl.eazysoftware.eazyrecyclingservice.domain.waste

data class WasteType(
  val name: String,
  val euralCode: EuralCode,
  val processingMethod: ProcessingMethod
)
