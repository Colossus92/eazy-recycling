package nl.eazysoftware.eazyrecyclingservice.repository.product

import nl.eazysoftware.eazyrecyclingservice.domain.model.product.ProductCategory
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemCategoryDto
import org.springframework.stereotype.Component
import java.util.*
import kotlin.time.toKotlinInstant

@Component
class ProductCategoryMapper {

    companion object {
        const val PRODUCT_TYPE = "PRODUCT"
    }

    fun toDto(domain: ProductCategory): CatalogItemCategoryDto {
        return CatalogItemCategoryDto(
            id = domain.id ?: UUID.randomUUID(),
            type = PRODUCT_TYPE,
            code = domain.code,
            name = domain.name,
            description = domain.description,
        )
    }

    fun toDomain(dto: CatalogItemCategoryDto): ProductCategory {
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
