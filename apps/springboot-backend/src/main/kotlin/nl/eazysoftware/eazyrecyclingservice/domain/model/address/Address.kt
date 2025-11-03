package nl.eazysoftware.eazyrecyclingservice.domain.model.address

data class Address(
  val streetName: StreetName,
  val buildingNumber: String,
  val buildingNumberAddition: String? = null,
  val postalCode: DutchPostalCode,
  val city: City,
  val country: String = "Nederland",
) {
  init {
    require(!buildingNumber.isBlank()){
      "Huisnummer moet een waarde hebben."
    }
  }
}


data class StreetName(val value: String) {
  init {
    require(value.isNotBlank()) { "De straatnaam is verplicht" }
    require(value.length <= 24) { "De straatnaam mag maximaal 24 tekens bevatten, maar was: $value" }
  }
}