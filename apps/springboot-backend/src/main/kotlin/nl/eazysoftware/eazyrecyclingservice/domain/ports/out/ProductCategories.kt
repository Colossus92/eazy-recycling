package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.product.ProductCategory

interface ProductCategories {
    fun getAllCategories(): List<ProductCategory>
    fun getCategoryById(id: Long): ProductCategory?
    fun createCategory(category: ProductCategory): ProductCategory
    fun updateCategory(id: Long, category: ProductCategory): ProductCategory
    fun deleteCategory(id: Long)
}
