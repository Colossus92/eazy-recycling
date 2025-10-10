package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PickupLocationRepository : JpaRepository<PickupLocationDto, String> {

  fun findDutchAddressByPostalCodeAndBuildingNumber(
    postalCode: String,
    buildingNumber: String
  ): PickupLocationDto.DutchAddressDto?

}
