package nl.eazysoftware.eazyrecyclingservice.application.query

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type"
)
@JsonSubTypes(
  JsonSubTypes.Type(value = PickupLocationView.DutchAddressView::class, name = "dutch_address"),
  JsonSubTypes.Type(value = PickupLocationView.ProximityDescriptionView::class, name = "proximity"),
  JsonSubTypes.Type(value = PickupLocationView.PickupCompanyView::class, name = "company"),
  JsonSubTypes.Type(value = PickupLocationView.ProjectLocationView::class, name = "project_location"),
  JsonSubTypes.Type(value = PickupLocationView.NoPickupView::class, name = "no_pickup")
)
sealed class PickupLocationView {
  data class DutchAddressView(
    val streetName: String,
    val postalCode: String,
    val buildingNumber: String,
    val buildingNumberAddition: String?,
    val city: String,
    val country: String
  ) : PickupLocationView()

  class NoPickupView : PickupLocationView()

  data class ProximityDescriptionView(
    val postalCodeDigits: String,
    val city: String,
    val description: String,
    val country: String
  ) : PickupLocationView()

  data class PickupCompanyView(
    val company: CompanyView
  ) : PickupLocationView()

  data class ProjectLocationView(
    val id: String,
    val company: CompanyView,
    val streetName: String,
    val postalCode: String,
    val buildingNumber: String,
    val buildingNumberAddition: String?,
    val city: String,
    val country: String
  ) : PickupLocationView()
}
