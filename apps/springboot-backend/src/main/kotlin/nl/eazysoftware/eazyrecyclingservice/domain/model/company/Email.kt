package nl.eazysoftware.eazyrecyclingservice.domain.model.company

data class Email(
  val value: String
) {
  init {
    require(value.matches(Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"))) {
      "Email moet een geldig emailadres zijn, maar was: $value"
    }
  }
}
