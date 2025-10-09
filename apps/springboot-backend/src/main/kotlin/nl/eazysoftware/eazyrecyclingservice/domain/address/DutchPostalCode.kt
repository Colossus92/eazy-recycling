package nl.eazysoftware.eazyrecyclingservice.domain.address

/**
 * Dutch postal code value object (4 digits + 2 letters, e.g., "1234AB")
 */
data class DutchPostalCode(val value: String) {
  init {
    require(value.matches(Regex("\\d{4} \\w{2}"))) {
      "Nederlandse postcode moet vier cijfers gevolgd door twee hoofdletters bevatten, gescheiden door een spatie, maar was: $value"
    }
  }
}
