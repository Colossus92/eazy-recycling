package nl.eazysoftware.eazyrecyclingservice.repository.company

import jakarta.persistence.EntityManager
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.City
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.StreetName
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyProjectLocation
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProjectLocationId
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import org.springframework.stereotype.Component

@Component
class ProjectLocationMapper(
  private val entityManager: EntityManager,
) {

  fun toDomain(dto: CompanyProjectLocationDto): CompanyProjectLocation {
    return CompanyProjectLocation(
      id = ProjectLocationId(dto.id),
      companyId = CompanyId(dto.company.id!!),
      address = Address(
        streetName = StreetName(dto.streetName),
        buildingNumber = dto.buildingNumber,
        buildingNumberAddition = dto.buildingNumberAddition,
        postalCode = DutchPostalCode(dto.postalCode),
        city = City(dto.city),
        country = dto.country
      ),
      createdAt = dto.createdAt.toKotlinInstant(),
      updatedAt = dto.updatedAt?.toKotlinInstant()
    )
  }

  fun toDto(domain: CompanyProjectLocation): CompanyProjectLocationDto {
    return CompanyProjectLocationDto(
      id = domain.id.uuid,
      streetName = domain.address.streetName.value,
      buildingNumber = domain.address.buildingNumber,
      buildingNumberAddition = domain.address.buildingNumberAddition,
      city = domain.address.city.value,
      postalCode = domain.address.postalCode.value,
      country = domain.address.country,
      company = entityManager.getReference(CompanyDto::class.java, domain.companyId.uuid),
      createdAt = domain.createdAt.toJavaInstant(),
      updatedAt = domain.updatedAt?.toJavaInstant() ?: domain.createdAt.toJavaInstant()
    )
  }
}
