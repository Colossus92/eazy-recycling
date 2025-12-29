package nl.eazysoftware.eazyrecyclingservice.repository.material

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.MaterialGroup
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemCategoryDto
import org.springframework.stereotype.Component
import java.util.*
import kotlin.time.toKotlinInstant

@Component
class MaterialGroupMapper {

    companion object {
        const val MATERIAL_TYPE = "MATERIAL"
    }

    fun toDto(domain: MaterialGroup): CatalogItemCategoryDto {
        return CatalogItemCategoryDto(
            id = domain.id ?: UUID.randomUUID(),
            type = MATERIAL_TYPE,
            code = domain.code,
            name = domain.name,
            description = domain.description,
        )
    }

    fun toDomain(dto: CatalogItemCategoryDto): MaterialGroup {
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
