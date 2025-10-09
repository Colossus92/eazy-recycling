package nl.eazysoftware.eazyrecyclingservice.domain.waste

data class ProcessingMethod(
  val code: String,
) {
  init {
      require(!code.isBlank()) {
        "Verwerkingsmethode moet een waarde hebben"
      }
  }
}
