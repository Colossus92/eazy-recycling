package nl.eazysoftware.eazyrecyclingservice.repository.product

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.domain.model.product.Product
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemCategoryDto
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateDto
import org.springframework.stereotype.Component
import kotlin.time.toKotlinInstant

@Component
class ProductMapper(
    private val entityManager: EntityManager
) {

    fun toDto(domain: Product): CatalogItemDto {
        return CatalogItemDto(
            id = domain.id,
            type = CatalogItemType.PRODUCT,
            code = domain.code,
            name = domain.name,
            category = domain.categoryId?.let { entityManager.getReference(CatalogItemCategoryDto::class.java, it) },
            unitOfMeasure = domain.unitOfMeasure,
            vatRate = entityManager.getReference(VatRateDto::class.java, domain.vatCode),
            consignorParty = null,
            defaultPrice = domain.defaultPrice,
            status = domain.status,
            purchaseAccountNumber = domain.purchaseAccountNumber,
            salesAccountNumber = domain.salesAccountNumber,
        )
    }

    fun toDomain(dto: CatalogItemDto): Product {
        return Product(
            id = dto.id,
            code = dto.code,
            name = dto.name,
            categoryId = dto.category?.id,
            categoryName = dto.category?.name,
            unitOfMeasure = dto.unitOfMeasure,
            vatCode = dto.vatRate.vatCode,
            salesAccountNumber = dto.salesAccountNumber,
            purchaseAccountNumber = dto.purchaseAccountNumber,
            status = dto.status,
            defaultPrice = dto.defaultPrice,
            createdAt = dto.createdAt?.toKotlinInstant(),
            createdBy = dto.createdBy,
            updatedAt = dto.updatedAt?.toKotlinInstant(),
            updatedBy = dto.updatedBy,
        )
    }
}
