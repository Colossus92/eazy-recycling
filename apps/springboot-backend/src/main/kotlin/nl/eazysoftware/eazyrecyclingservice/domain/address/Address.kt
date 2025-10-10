package nl.eazysoftware.eazyrecyclingservice.domain.address

data class Address(
  val streetName: String,
  val buildingNumber: String,
  val buildingNumberAddition: String? = null,
  val postalCode: DutchPostalCode,
  val city: String,
  val country: String = "Nederland",
) {
  init {
    require(!streetName.isBlank()){
      "Straatnaam moet een waarde hebben."
    }
    require(!buildingNumber.isBlank()){
      "Huisnummer moet een waarde hebben."
    }
    require(!city.isBlank()){
      "Woonplaats moet een waarde hebben."
    }
  }
}
