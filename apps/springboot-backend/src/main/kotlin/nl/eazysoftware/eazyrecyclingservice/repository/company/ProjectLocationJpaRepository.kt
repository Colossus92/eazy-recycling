package nl.eazysoftware.eazyrecyclingservice.repository.company

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyProjectLocation
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

interface ProjectLocationJpaRepository : JpaRepository<CompanyProjectLocationDto, UUID> {

  @Query("SELECT COUNT(p) > 0 FROM CompanyProjectLocationDto p WHERE p.company.id = :companyId AND p.postalCode = :postalCode AND p.buildingNumber = :buildingNumber")
  fun existsByCompanyIdAndPostalCodeAndBuildingNumber(
    @Param("companyId") companyId: UUID,
    @Param("postalCode") postalCode: String,
    @Param("buildingNumber") buildingNumber: String
  ): Boolean
}

@Repository
class ProjectLocationRepository(
  private val jpaRepository: ProjectLocationJpaRepository,
  private val locationMapper: ProjectLocationMapper,
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

  override fun create(location: CompanyProjectLocation) {
    jpaRepository.save(locationMapper.toDto(location))
  }

  override fun findAll() =
    jpaRepository.findAll()
      .map { dto -> locationMapper.toDomain(dto) }

  override fun findById(id: UUID) =
    jpaRepository.findByIdOrNull(id)
      ?.let { locationMapper.toDomain(it) }

  override fun deleteById(id: UUID) {
    jpaRepository.deleteById(id)
  }

  override fun update(location: CompanyProjectLocation) {
    locationMapper.toDto(location)
      .let { jpaRepository.save(it) }
  }
}
