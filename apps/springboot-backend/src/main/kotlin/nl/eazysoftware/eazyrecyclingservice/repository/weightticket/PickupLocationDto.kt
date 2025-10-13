package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "pickup_locations")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "location_type", discriminatorType = DiscriminatorType.STRING)
class PickupLocationDto(
  @Id
  val id: String = UUID.randomUUID().toString()
) {
  @Entity
  @DiscriminatorValue("DUTCH_ADDRESS")
  class DutchAddressDto(
    @Column(name = "street_name")
    var streetName: String,

    @Column(name = "building_number")
    var buildingNumber: String,

    @Column(name = "building_number_addition")
    var buildingNumberAddition: String?,

    @Column(name = "postal_code")
    var postalCode: String,

    @Column
    var city: String,

    @Column
    var country: String,
  ) : PickupLocationDto()

  @Entity
  @DiscriminatorValue("PROXIMITY_DESC")
  class ProximityDescriptionDto(
    @Column(name = "proximity_description")
    var description: String,
    @Column(name = "postal_code")
    var postalCode: String,
    @Column(name = "city")
    var city: String,
    @Column(name = "country")
    var country: String,
  ) : PickupLocationDto()

  @Entity
  @DiscriminatorValue("NO_PICKUP")
  class NoPickupLocationDto() : PickupLocationDto("NO_PICKUP")
}
