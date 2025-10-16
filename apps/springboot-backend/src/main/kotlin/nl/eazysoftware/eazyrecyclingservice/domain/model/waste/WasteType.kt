package nl.eazysoftware.eazyrecyclingservice.domain.model.waste

data class WasteType(
  val name: String,
  val euralCode: EuralCode,
  val processingMethod: ProcessingMethod
)
