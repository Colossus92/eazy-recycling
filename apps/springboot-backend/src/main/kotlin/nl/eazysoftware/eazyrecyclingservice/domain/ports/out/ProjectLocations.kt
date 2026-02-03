package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyProjectLocation
import java.util.*

interface ProjectLocations {

  fun existsByCompanyIdAndPostalCodeAndBuildingNumber(
    companyId: CompanyId,
    postalCode: DutchPostalCode,
    buildingNumber: String,
  ): Boolean

  fun findByCompanyIdAndPostalCodeAndBuildingNumber(
    companyId: CompanyId,
    postalCode: DutchPostalCode,
    buildingNumber: String,
  ): CompanyProjectLocation?

  fun create(location: CompanyProjectLocation)

  fun findAll(): List<CompanyProjectLocation>

  fun findById(id: UUID): CompanyProjectLocation?

  fun deleteById(id: UUID)

  fun update(location: CompanyProjectLocation)

}
