package nl.eazysoftware.eazyrecyclingservice.repository.material

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.MaterialGroup
import org.springframework.stereotype.Component
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Component
class MaterialGroupMapper {

    fun toDto(domain: MaterialGroup): MaterialGroupDto {
        return MaterialGroupDto(
            id = domain.id,
            code = domain.code,
            name = domain.name,
            description = domain.description,
            createdAt = domain.createdAt?.toJavaInstant() ?: java.time.Instant.now(),
            updatedAt = domain.updatedAt?.toJavaInstant()
        )
    }

    fun toDomain(dto: MaterialGroupDto): MaterialGroup {
        return MaterialGroup(
            id = dto.id,
            code = dto.code,
            name = dto.name,
            description = dto.description,
            createdAt = dto.createdAt.toKotlinInstant(),
            updatedAt = dto.updatedAt?.toKotlinInstant()
        )
    }
}
