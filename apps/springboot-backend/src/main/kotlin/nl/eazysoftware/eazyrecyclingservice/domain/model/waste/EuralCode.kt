package nl.eazysoftware.eazyrecyclingservice.domain.model.waste

data class EuralCode(
  val code: String
) {

  init {
      require(!code.isBlank()) {
        "Euralcode moet een waarde hebben"
      }
  }
}
