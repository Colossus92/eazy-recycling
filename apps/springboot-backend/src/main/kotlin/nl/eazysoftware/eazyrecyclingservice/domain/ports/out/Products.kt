package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.product.Product
import java.util.*

interface Products {
    fun getAllProducts(): List<Product>
    fun getProductById(id: UUID): Product?
    fun getActiveProducts(): List<Product>
    fun searchProducts(query: String, limit: Int = 50): List<Product>
    fun createProduct(product: Product): Product
    fun updateProduct(id: UUID, product: Product): Product
    fun deleteProduct(id: UUID)
}
