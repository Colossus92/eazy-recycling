package nl.eazysoftware.eazyrecyclingservice.domain.address

data class Address(
  val streetName: String,
  val houseNumber: String,
  val houseNumberAddition: String? = null,
  val postalCode: DutchPostalCode,
  val city: String,
  val country: String = "Nederland",
) {
  init {
    require(!streetName.isBlank()){
      "Straatnaam moet een waarde hebben."
    }
    require(!houseNumber.isBlank()){
      "Huisnummer moet een waarde hebben."
    }
    require(!city.isBlank()){
      "Woonplaats moet een waarde hebben."
    }
  }
}
