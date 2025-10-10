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

    @Column(name = "building_number", nullable = false)
    var buildingNumber: String = "",

    @Column(name = "building_number_addition")
    var buildingNumberAddition: String? = null,

    @Column(name = "postal_code", nullable = false)
    var postalCode: String = "",

    @Column(nullable = false)
    var country: String = "",
  ) : PickupLocationDto()

  @Entity
  @DiscriminatorValue("PROXIMITY_DESC")
  class ProximityDescriptionDto(
    @Column(name = "proximity_description", nullable = false)
    var description: String = "",
    @Column(name = "postal_code", nullable = false)
    var postalCode: String = "",
    @Column(name = "city", nullable = false)
    var city: String = "",
    @Column(name = "country", nullable = false)
    var country: String = "",
  ) : PickupLocationDto()

  @Entity
  @DiscriminatorValue("NO_PICKUP")
  class NoPickupLocationDto() : PickupLocationDto("NO_PICKUP")
}
