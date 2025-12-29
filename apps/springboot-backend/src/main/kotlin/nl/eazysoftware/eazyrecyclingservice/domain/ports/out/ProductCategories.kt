package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.product.ProductCategory
import java.util.*

interface ProductCategories {
    fun getAllCategories(): List<ProductCategory>
    fun getCategoryById(id: UUID): ProductCategory?
    fun createCategory(category: ProductCategory): ProductCategory
    fun updateCategory(id: UUID, category: ProductCategory): ProductCategory
    fun deleteCategory(id: UUID)
}
