package nl.eazysoftware.eazyrecyclingservice.domain.transport

import java.time.ZonedDateTime

data class Truck(
  val licensePlate: String,
  val brand: String,
  val description: String,
  val updatedAt: ZonedDateTime?
) {
  init {
      require(licensePlate.isNotBlank()) { "Het kenteken is verplicht" }
      require(brand.isNotBlank()) { "Het merk is verplicht" }
      require(description.isNotBlank()) { "De beschrijving is verplicht" }
  }
}
