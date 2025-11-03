package nl.eazysoftware.eazyrecyclingservice.domain.model.address

data class Address(
  val streetName: String,
  val buildingNumber: String,
  val buildingNumberAddition: String? = null,
  val postalCode: DutchPostalCode,
  val city: City,
  val country: String = "Nederland",
) {
  init {
    require(!streetName.isBlank()){
      "Straatnaam moet een waarde hebben."
    }
    require(!buildingNumber.isBlank()){
      "Huisnummer moet een waarde hebben."
    }
  }
}
