package nl.eazysoftware.eazyrecyclingservice.repository.product

import nl.eazysoftware.eazyrecyclingservice.domain.model.product.Product
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Products
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

interface ProductJpaRepository : JpaRepository<ProductDto, Long> {

    fun findByStatus(status: String): List<ProductDto>

    @Query(
        """
        SELECT p FROM ProductDto p 
        LEFT JOIN FETCH p.category 
        WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) 
           OR LOWER(p.code) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY p.name
        """
    )
    fun search(query: String): List<ProductDto>

    @Query(
        """
        SELECT p FROM ProductDto p 
        LEFT JOIN FETCH p.category 
        WHERE p.id = :id
        """
    )
    fun findByIdWithCategory(@Param("id") id: Long): ProductDto?
}

@Repository
class ProductRepository(
    private val jpaRepository: ProductJpaRepository,
    private val mapper: ProductMapper
) : Products {

    override fun getAllProducts(): List<Product> {
        return jpaRepository.findAll().map { mapper.toDomain(it) }
    }

    override fun getProductById(id: Long): Product? {
        val dto = jpaRepository.findByIdWithCategory(id) ?: return null
        // Force initialization of the category if it's a proxy
        dto.category?.name
        return mapper.toDomain(dto)
    }

    override fun getActiveProducts(): List<Product> {
        return jpaRepository.findByStatus("ACTIVE").map { mapper.toDomain(it) }
    }

    override fun searchProducts(query: String, limit: Int): List<Product> {
        return jpaRepository.search(query)
            .take(limit)
            .map { mapper.toDomain(it) }
    }

    override fun createProduct(product: Product): Product {
        val dto = mapper.toDto(product.copy(id = null))
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun updateProduct(id: Long, product: Product): Product {
        val dto = mapper.toDto(product.copy(id = id))
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun deleteProduct(id: Long) {
        jpaRepository.deleteById(id)
    }
}
