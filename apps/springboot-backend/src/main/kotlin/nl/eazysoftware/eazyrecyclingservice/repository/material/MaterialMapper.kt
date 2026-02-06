package nl.eazysoftware.eazyrecyclingservice.repository.material

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.domain.model.material.Material
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemCategoryDto
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateDto
import org.springframework.stereotype.Component
import java.util.*
import kotlin.time.toKotlinInstant

@Component
class MaterialMapper(
    private val entityManager: EntityManager
) {

    fun toDto(domain: Material): CatalogItemDto {
        return CatalogItemDto(
            id = domain.id ?: UUID.randomUUID(),
            type = CatalogItemType.MATERIAL,
            code = domain.code,
            name = domain.name,
            category = domain.materialGroupId?.let { entityManager.getReference(CatalogItemCategoryDto::class.java, it) },
            unitOfMeasure = domain.unitOfMeasure,
            vatRate = entityManager.getReference(VatRateDto::class.java, domain.vatRateId),
            consignorParty = null,
            defaultPrice = null,
            status = domain.status,
            purchaseAccountNumber = domain.purchaseAccountNumber,
            salesAccountNumber = domain.salesAccountNumber,
        )
    }

    fun toDomain(dto: CatalogItemDto): Material {
        return Material(
            id = dto.id,
            code = dto.code,
            name = dto.name,
            materialGroupId = dto.category?.id,
            unitOfMeasure = dto.unitOfMeasure,
            vatRateId = dto.vatRate.id,
            salesAccountNumber = dto.salesAccountNumber,
            purchaseAccountNumber = dto.purchaseAccountNumber,
            status = dto.status,
            createdAt = dto.createdAt?.toKotlinInstant(),
            createdBy = dto.createdBy,
            updatedAt = dto.updatedAt?.toKotlinInstant(),
            updatedBy = dto.updatedBy,
        )
    }
}
