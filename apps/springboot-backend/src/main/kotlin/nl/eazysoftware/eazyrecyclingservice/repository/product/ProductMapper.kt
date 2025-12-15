package nl.eazysoftware.eazyrecyclingservice.repository.product

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.domain.model.product.Product
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateDto
import org.springframework.stereotype.Component
import kotlin.time.toKotlinInstant

@Component
class ProductMapper(
    private val entityManager: EntityManager
) {

    fun toDto(domain: Product): ProductDto {
        return ProductDto(
            id = domain.id,
            code = domain.code,
            name = domain.name,
            category = domain.categoryId?.let { entityManager.getReference(ProductCategoryDto::class.java, it) },
            unitOfMeasure = domain.unitOfMeasure,
            vatRate = entityManager.getReference(VatRateDto::class.java, domain.vatCode),
            glAccountCode = domain.glAccountCode,
            status = domain.status,
            defaultPrice = domain.defaultPrice,
            description = domain.description,
        )
    }

    fun toDomain(dto: ProductDto): Product {
        return Product(
            id = dto.id,
            code = dto.code,
            name = dto.name,
            categoryId = dto.category?.id,
            categoryName = dto.category?.name,
            unitOfMeasure = dto.unitOfMeasure,
            vatCode = dto.vatRate.vatCode,
            glAccountCode = dto.glAccountCode,
            status = dto.status,
            defaultPrice = dto.defaultPrice,
            description = dto.description,
            createdAt = dto.createdAt?.toKotlinInstant(),
            createdBy = dto.createdBy,
            updatedAt = dto.updatedAt?.toKotlinInstant(),
            updatedBy = dto.updatedBy,
        )
    }
}
