package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.product.Product

interface Products {
    fun getAllProducts(): List<Product>
    fun getProductById(id: Long): Product?
    fun getActiveProducts(): List<Product>
    fun searchProducts(query: String, limit: Int = 50): List<Product>
    fun createProduct(product: Product): Product
    fun updateProduct(id: Long, product: Product): Product
    fun deleteProduct(id: Long)
}
