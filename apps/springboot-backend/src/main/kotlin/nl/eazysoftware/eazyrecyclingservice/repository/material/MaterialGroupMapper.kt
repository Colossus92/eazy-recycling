package nl.eazysoftware.eazyrecyclingservice.repository.material

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.MaterialGroup
import org.springframework.stereotype.Component
import kotlin.time.toKotlinInstant

@Component
class MaterialGroupMapper {

    fun toDto(domain: MaterialGroup): MaterialGroupDto {
        return MaterialGroupDto(
            id = domain.id,
            code = domain.code,
            name = domain.name,
            description = domain.description,
            // Audit fields are managed by Spring Data JPA
        )
    }

    fun toDomain(dto: MaterialGroupDto): MaterialGroup {
        return MaterialGroup(
            id = dto.id,
            code = dto.code,
            name = dto.name,
            description = dto.description,
            createdAt = dto.createdAt?.toKotlinInstant(),
            createdBy = dto.createdBy,
            updatedAt = dto.updatedAt?.toKotlinInstant(),
            updatedBy = dto.updatedBy,
        )
    }
}
