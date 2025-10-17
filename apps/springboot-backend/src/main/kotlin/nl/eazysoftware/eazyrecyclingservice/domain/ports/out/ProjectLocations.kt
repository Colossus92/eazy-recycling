package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import java.util.*

interface ProjectLocations {

  fun existsByCompanyIdAndPostalCodeAndBuildingNumber(
    companyId: CompanyId,
    postalCode: DutchPostalCode,
    buildingNumber: String,
  ): Boolean

  fun create(location: Location.ProjectLocation)

  fun findAll(): List<Location.ProjectLocation>

  fun findById(id: UUID): Location.ProjectLocation?

  fun deleteById(id: UUID)

  fun update(location: Location.ProjectLocation)

}
