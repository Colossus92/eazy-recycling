package nl.eazysoftware.eazyrecyclingservice.domain.model.waste

data class ProcessingMethod(
  val code: String,
) {
  init {
      require(!code.isBlank()) {
        "Verwerkingsmethode moet een waarde hebben"
      }
  }
}
