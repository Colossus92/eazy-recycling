package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.product.Product
import nl.eazysoftware.eazyrecyclingservice.repository.product.ProductQueryResult
import java.util.*

interface Products {
    fun getAllProducts(): List<Product>
    fun getAllProductsWithDetails(): List<ProductQueryResult>
    fun getProductById(id: UUID): Product?
    fun getProductWithDetailsById(id: UUID): ProductQueryResult?
    fun getActiveProducts(): List<Product>
    fun searchProducts(query: String, limit: Int = 50): List<Product>
    fun createProduct(product: Product): Product
    fun updateProduct(id: UUID, product: Product): Product
    fun deleteProduct(id: UUID)
}
