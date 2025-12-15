package nl.eazysoftware.eazyrecyclingservice.repository.product

import nl.eazysoftware.eazyrecyclingservice.domain.model.product.ProductCategory
import org.springframework.stereotype.Component
import kotlin.time.toKotlinInstant

@Component
class ProductCategoryMapper {

    fun toDto(domain: ProductCategory): ProductCategoryDto {
        return ProductCategoryDto(
            id = domain.id,
            code = domain.code,
            name = domain.name,
            description = domain.description,
        )
    }

    fun toDomain(dto: ProductCategoryDto): ProductCategory {
        return ProductCategory(
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
