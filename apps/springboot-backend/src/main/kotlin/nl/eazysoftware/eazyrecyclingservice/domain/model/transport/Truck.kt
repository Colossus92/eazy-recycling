package nl.eazysoftware.eazyrecyclingservice.domain.model.transport

import java.time.ZonedDateTime

data class Truck(
  val licensePlate: LicensePlate,
  val brand: String,
  val description: String,
  val updatedAt: ZonedDateTime?
) {
  init {
      require(brand.isNotBlank()) { "Het merk is verplicht" }
      require(description.isNotBlank()) { "De beschrijving is verplicht" }
  }
}

data class LicensePlate(val value: String) {
  init {
    require(value.isNotBlank()) { "Het kenteken is verplicht" }
  }
}
