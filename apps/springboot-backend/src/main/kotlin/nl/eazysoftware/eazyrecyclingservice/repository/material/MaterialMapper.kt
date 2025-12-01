package nl.eazysoftware.eazyrecyclingservice.repository.material

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.domain.model.material.Material
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateDto
import org.springframework.stereotype.Component
import kotlin.time.toKotlinInstant

@Component
class MaterialMapper(
    private val entityManager: EntityManager
) {

    fun toDto(domain: Material): MaterialDto {
        return MaterialDto(
            id = domain.id,
            code = domain.code,
            name = domain.name,
            materialGroup = entityManager.getReference(MaterialGroupDto::class.java, domain.materialGroupId),
            unitOfMeasure = domain.unitOfMeasure,
            vatRate = entityManager.getReference(VatRateDto::class.java, domain.vatCode),
            status = domain.status,
            // Audit fields are managed by Spring Data JPA
        )
    }

    fun toDomain(dto: MaterialDto): Material {
        return Material(
            id = dto.id,
            code = dto.code,
            name = dto.name,
            materialGroupId = dto.materialGroup.id!!,
            unitOfMeasure = dto.unitOfMeasure,
            vatCode = dto.vatRate.vatCode,
            status = dto.status,
            createdAt = dto.createdAt?.toKotlinInstant(),
            createdBy = dto.createdBy,
            updatedAt = dto.updatedAt?.toKotlinInstant(),
            updatedBy = dto.updatedBy,
        )
    }
}
