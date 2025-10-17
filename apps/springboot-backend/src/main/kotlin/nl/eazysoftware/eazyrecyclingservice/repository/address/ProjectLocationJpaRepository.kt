package nl.eazysoftware.eazyrecyclingservice.repository.address

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto.PickupProjectLocationDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

interface ProjectLocationJpaRepository : JpaRepository<PickupLocationDto, String> {

  @Query("SELECT COUNT(p) > 0 FROM PickupLocationDto p WHERE TYPE(p) = 'PROJECT_LOCATION' AND p.companyId = :companyId AND p.postalCode = :postalCode AND p.buildingNumber = :buildingNumber")
  fun existsByCompanyIdAndPostalCodeAndBuildingNumber(
    @Param("companyId") companyId: UUID,
    @Param("postalCode") postalCode: String,
    @Param("buildingNumber") buildingNumber: String
  ): Boolean

  @Query("SELECT p FROM PickupLocationDto p WHERE TYPE(p) = 'PROJECT_LOCATION'")
  fun findAllProjectLocations(): List<PickupProjectLocationDto>
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
    jpaRepository.findAllProjectLocations()
      .map { dto -> locationMapper.toDomain(dto) as Location.ProjectLocation }

  override fun findById(id: UUID): Location.ProjectLocation? {
    val dto = jpaRepository.findById(id.toString()).orElse(null) ?: return null
    return if (dto is PickupProjectLocationDto) {
      locationMapper.toDomain(dto) as Location.ProjectLocation
    } else {
      null
    }
  }

  override fun deleteById(id: UUID) {
    jpaRepository.deleteById(id.toString())
  }


  override fun update(location: Location.ProjectLocation) {
    locationMapper.toDto(location)
      .let { jpaRepository.save(it) }
  }
}
