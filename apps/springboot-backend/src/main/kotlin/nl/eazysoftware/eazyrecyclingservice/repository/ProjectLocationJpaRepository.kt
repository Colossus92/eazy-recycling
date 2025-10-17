package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto.PickupProjectLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationMapper
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

interface ProjectLocationJpaRepository : JpaRepository<PickupProjectLocationDto, UUID> {

  @Query("SELECT COUNT(b) > 0 FROM PickupProjectLocationDto b WHERE b.companyId = :companyId AND b.postalCode = :postalCode AND b.buildingNumber = :buildingNumber")
  fun existsByCompanyIdAndPostalCodeAndBuildingNumber(
    @Param("companyId") companyId: UUID,
    @Param("postalCode") postalCode: String,
    @Param("buildingNumber") buildingNumber: String
  ): Boolean
}

@Repository
class ProjectLocationRepository(
  private val jpaRepository: ProjectLocationJpaRepository,
  private val locationMapper: PickupLocationMapper,
) : ProjectLocations {
  override fun existsByCompanyIdAndPostalCodeAndBuildingNumber(
    companyId: CompanyId,
    postalCode: DutchPostalCode,
    buildingNumber: String
  ) =
    jpaRepository.existsByCompanyIdAndPostalCodeAndBuildingNumber(
      companyId.uuid,
      postalCode.value,
      buildingNumber
    )

  override fun create(location: Location.ProjectLocation) {
    jpaRepository.save(locationMapper.toDto(location) as PickupProjectLocationDto)
  }

  override fun findAll() =
    jpaRepository.findAll()
      .map { dto -> locationMapper.toDomain(dto) as Location.ProjectLocation }


}
