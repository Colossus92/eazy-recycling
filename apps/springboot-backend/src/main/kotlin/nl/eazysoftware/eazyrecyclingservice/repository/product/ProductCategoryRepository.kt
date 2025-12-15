package nl.eazysoftware.eazyrecyclingservice.repository.product

import nl.eazysoftware.eazyrecyclingservice.domain.model.product.ProductCategory
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProductCategories
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

interface ProductCategoryJpaRepository : JpaRepository<ProductCategoryDto, Long>

@Repository
class ProductCategoryRepository(
    private val jpaRepository: ProductCategoryJpaRepository,
    private val mapper: ProductCategoryMapper
) : ProductCategories {

    override fun getAllCategories(): List<ProductCategory> {
        return jpaRepository.findAll().map { mapper.toDomain(it) }
    }

    override fun getCategoryById(id: Long): ProductCategory? {
        return jpaRepository.findByIdOrNull(id)?.let { mapper.toDomain(it) }
    }

    override fun createCategory(category: ProductCategory): ProductCategory {
        val dto = mapper.toDto(category.copy(id = null))
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun updateCategory(id: Long, category: ProductCategory): ProductCategory {
        val dto = mapper.toDto(category.copy(id = id))
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun deleteCategory(id: Long) {
        jpaRepository.deleteById(id)
    }
}
