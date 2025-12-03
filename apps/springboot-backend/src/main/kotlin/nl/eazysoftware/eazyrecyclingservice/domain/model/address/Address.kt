package nl.eazysoftware.eazyrecyclingservice.domain.model.address

data class Address(
  val streetName: StreetName,
  val buildingNumber: String,
  val buildingNumberAddition: String? = null,
  val postalCode: DutchPostalCode,
  val city: City,
  val country: String = "Nederland",
) {
  // Domain constraints relaxed per ADR-0017 to allow Exact Online sync with incomplete data.
  // Validation is enforced at controller level for user-created data and by LMA validation for declarations.

  fun toAddressLine() = "${streetName.value} $buildingNumber${buildingNumberAddition ?: ""}, ${city.value}"
}


data class StreetName(val value: String)
// Domain constraints relaxed per ADR-0017 to allow Exact Online sync with incomplete data.

data class City(val value: String)
// Domain constraints relaxed per ADR-0017 to allow Exact Online sync with incomplete data.
