package nl.eazysoftware.eazyrecyclingservice.repository.material

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.domain.model.material.MaterialPrice
import org.springframework.stereotype.Component
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Component
class MaterialPriceMapper(
    private val entityManager: EntityManager
) {

    fun toDomain(dto: MaterialPriceDto): MaterialPrice {
        return MaterialPrice(
            id = dto.id,
            materialId = dto.material.id!!,
            materialCode = dto.material.code,
            materialName = dto.material.name,
            price = dto.price,
            currency = dto.currency,
            validFrom = dto.validFrom.toKotlinInstant(),
            validTo = dto.validTo?.toKotlinInstant(),
            createdAt = dto.createdAt?.toKotlinInstant(),
            createdBy = dto.createdBy,
            updatedAt = dto.updatedAt?.toKotlinInstant(),
            updatedBy = dto.updatedBy,
        )
    }

    fun toDto(domain: MaterialPrice): MaterialPriceDto {
        val material = entityManager.getReference(MaterialDto::class.java, domain.materialId)

        return MaterialPriceDto(
            id = domain.id,
            material = material,
            price = domain.price,
            currency = domain.currency,
            validFrom = domain.validFrom.toJavaInstant(),
            validTo = domain.validTo?.toJavaInstant(),
        )
    }
}
