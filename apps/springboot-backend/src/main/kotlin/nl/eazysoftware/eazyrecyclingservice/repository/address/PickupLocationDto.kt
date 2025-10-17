package nl.eazysoftware.eazyrecyclingservice.repository.address

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationType.COMPANY
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationType.DUTCH_ADDRESS
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationType.NO_PICKUP
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationType.PROJECT_LOCATION
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationType.PROXIMITY_DESC
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
  @DiscriminatorValue(DUTCH_ADDRESS)
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
  @DiscriminatorValue(PROXIMITY_DESC)
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
  @DiscriminatorValue(NO_PICKUP)
  class NoPickupLocationDto() : PickupLocationDto(NO_PICKUP)

  @Entity
  @DiscriminatorValue(COMPANY)
  class PickupCompanyDto(
    @Column(name = "company_id")
    var companyId: UUID
  ) : PickupLocationDto()


  @Entity
  @DiscriminatorValue(PROJECT_LOCATION)
  class PickupProjectLocationDto(
    @Column(name = "company_id")
    var companyId: UUID,

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
}

object PickupLocationType {
    const val DUTCH_ADDRESS: String = "DUTCH_ADDRESS"
    const val PROXIMITY_DESC: String = "PROXIMITY_DESC"
    const val NO_PICKUP: String = "NO_PICKUP"
    const val COMPANY: String = "COMPANY"
    const val PROJECT_LOCATION: String = "PROJECT_LOCATION"
}
