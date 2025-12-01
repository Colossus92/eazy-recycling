package nl.eazysoftware.eazyrecyclingservice.repository.truck

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.Truck
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.TruckDto
import org.springframework.stereotype.Component

@Component
class TruckMapper(
  private val entityManager: EntityManager
) {

  fun toDomain(dto: TruckDto): Truck {
    return Truck(
      licensePlate = LicensePlate(dto.licensePlate),
      brand = dto.brand ?: "",
      description = dto.description ?: "",
      displayName = dto.getDisplayName(),
      carrierPartyId = dto.carrierParty?.id?.let { CompanyId(it) },
      createdAt = dto.createdAt,
      createdBy = dto.createdBy,
      updatedAt = dto.updatedAt,
      updatedBy = dto.updatedBy
    )
  }

  fun toDto(domain: Truck): TruckDto {
    return TruckDto(
      licensePlate = domain.licensePlate.value.uppercase(),
      brand = domain.brand,
      description = domain.description,
      carrierParty = domain.carrierPartyId?.let {
        entityManager.getReference(CompanyDto::class.java, it.uuid)
      }
      // Audit fields are managed by Spring Data JPA
    )
  }
}
