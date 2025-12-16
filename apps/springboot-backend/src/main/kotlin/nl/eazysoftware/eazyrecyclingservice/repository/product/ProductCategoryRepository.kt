package nl.eazysoftware.eazyrecyclingservice.repository.product

import nl.eazysoftware.eazyrecyclingservice.domain.model.product.ProductCategory
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProductCategories
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemCategoryJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.product.ProductCategoryMapper.Companion.PRODUCT_TYPE
import org.springframework.stereotype.Repository

@Repository
class ProductCategoryRepository(
    private val jpaRepository: CatalogItemCategoryJpaRepository,
    private val mapper: ProductCategoryMapper
) : ProductCategories {

    override fun getAllCategories(): List<ProductCategory> {
        return jpaRepository.findByType(PRODUCT_TYPE).map { mapper.toDomain(it) }
    }

    override fun getCategoryById(id: Long): ProductCategory? {
        return jpaRepository.findByIdAndType(id, PRODUCT_TYPE)?.let { mapper.toDomain(it) }
    }

    override fun createCategory(category: ProductCategory): ProductCategory {
        val dto = mapper.toDto(category.copy(id = null))
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun updateCategory(id: Long, category: ProductCategory): ProductCategory {
        val existing = jpaRepository.findByIdAndType(id, PRODUCT_TYPE)
            ?: throw NoSuchElementException("Productcategorie met id $id niet gevonden")
        existing.code = category.code
        existing.name = category.name
        existing.description = category.description
        val saved = jpaRepository.save(existing)
        return mapper.toDomain(saved)
    }

    override fun deleteCategory(id: Long) {
        val existing = jpaRepository.findByIdAndType(id, PRODUCT_TYPE)
            ?: throw NoSuchElementException("Productcategorie met id $id niet gevonden")
        jpaRepository.delete(existing)
    }
}
