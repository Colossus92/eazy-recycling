package nl.eazysoftware.eazyrecyclingservice.repository.address

import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto.DutchAddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto.PickupProjectLocationDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PickupLocationRepository : JpaRepository<PickupLocationDto, String> {

  fun findDutchAddressByPostalCodeAndBuildingNumber(
    postalCode: String,
    buildingNumber: String
  ): DutchAddressDto?

  fun findPickupProjectLocationByPostalCodeAndBuildingNumberAndCompanyId(
    postalCode: String,
    buildingNumber: String,
    companyId: UUID,
  ): PickupProjectLocationDto?

    fun findCompanyByCompanyId(id: UUID?): PickupLocationDto.PickupCompanyDto?

}
