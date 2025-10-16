package nl.eazysoftware.eazyrecyclingservice.domain.model.address

/**
 * Dutch postal code value object (4 digits + 2 letters, e.g., "1234AB")
 * Accepts input with or without space and normalizes to format without space.
 */
data class DutchPostalCode(val input: String) {
  val value: String = input.replace(" ", "").uppercase()

  init {
    require(value.matches(Regex("^[1-9][0-9]{3}[A-Z]{2}$"))) {
      "Nederlandse postcode moet het formaat 1234AB hebben (vier cijfers gevolgd door twee hoofdletters), maar was: $input"
    }
  }
}
