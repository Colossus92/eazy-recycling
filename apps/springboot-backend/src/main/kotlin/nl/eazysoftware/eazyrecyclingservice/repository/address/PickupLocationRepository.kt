package nl.eazysoftware.eazyrecyclingservice.repository.address

import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto.DutchAddressDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PickupLocationRepository : JpaRepository<PickupLocationDto, String> {

  fun findDutchAddressByPostalCodeAndBuildingNumber(
    postalCode: String,
    buildingNumber: String
  ): DutchAddressDto?

  @Query("SELECT p FROM PickupLocationDto p WHERE TYPE(p) = 'COMPANY' AND p.company.id = :companyId")
  fun findCompanyByCompanyId(companyId: UUID?): PickupLocationDto?

}
