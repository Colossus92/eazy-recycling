package nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill

import jakarta.persistence.Embeddable

@Embeddable
data class AddressDto(
  var streetName: String,
  var buildingNumber: String,
  var buildingNumberAddition: String? = null,
  var city: String,
  var postalCode: String,
  var country: String
)
